# Email y transacciones en CompanyService

> **Estado**: ✅ Implementado (Mayo 2026)

---

## Resumen del patrón implementado

Los emails de aprobación y rechazo de empresas se envían **siempre después del commit de BD**
mediante `@TransactionalEventListener(phase = AFTER_COMMIT) + @Async`.

```
HTTP request
    └── CompanyService (transacción activa)
            ├── Cambios en BD (empresa, usuarios)
            ├── eventPublisher.publishEvent(CompanyEvent)   ← guardado, no enviado aún
            └── COMMIT

    Después del commit (hilo async del pool):
    CompanyEventListener.onCompanyEvent(event)
            ├── emailService.enviarAprobacion() o enviarRechazo()
            └── companyRepository.updateEmailEnviado(id, true)   ← nueva mini-transacción
```

**Ventajas sobre el try-catch anterior:**

| | try-catch (anterior) | @TransactionalEventListener (actual) |
|---|---|---|
| Email se envía si el commit falla | ✗ posible | ✓ nunca |
| Email se envía si el commit tiene éxito | ✓ | ✓ |
| Fallo SMTP revierte cambio de estado | ✗ (con el fix) | ✓ nunca |
| HTTP response espera al servidor SMTP | ✓ bloquea | ✗ async |
| Visibilidad del fallo para el admin | ✗ solo logs | ✓ campo `emailEnviado` |

---

## 1. Config SMTP: puerto 587 (STARTTLS) — Render

El puerto **465** (SMTPS) está bloqueado en Render. La configuración correcta es:

```properties
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USER}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
app.mail.from=${MAIL_USER}
```

> `MAIL_PASSWORD` debe ser una **App Password de Google** (16 caracteres).
> Requiere que la cuenta de Gmail tenga la **verificación en dos pasos activada**.
> La contraseña normal de la cuenta no funciona con SMTP.

---

## 2. Piezas del patrón

### `CompanyEvent` · `company/event/CompanyEvent.java`

Record inmutable publicado por `CompanyService` antes de salir del método `@Transactional`.
Transporta todos los datos necesarios para el email — crítico en el rechazo, donde la empresa
se elimina de BD antes del commit.

```java
public record CompanyEvent(
        UUID empresaId,
        String nombreEmpresa,
        String emailAdmin,
        String nombreAdmin,
        EstadoSolicitud estadoAnterior,
        EstadoSolicitud estadoNuevo
) {}
```

**Casos cubiertos:**

| `estadoAnterior` | `estadoNuevo` | Acción en el listener |
|---|---|---|
| `PENDIENTE` | `APROBADA` | `enviarAprobacion` + `updateEmailEnviado(true)` |
| `PENDIENTE` | `RECHAZADA` | `enviarRechazo` (empresa ya eliminada, no hay update) |

---

### `CompanyEventListener` · `company/event/CompanyEventListener.java`

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onCompanyEvent(CompanyEvent event) { ... }
```

- **`@Async`**: corre en el thread pool de Spring (`@EnableAsync` en `BackendApplication`),
  liberando el hilo HTTP inmediatamente después del commit.
- **`AFTER_COMMIT`**: garantiza que el evento solo se procesa si la transacción completó con éxito.
- **`try/catch (MailException)`**: un fallo SMTP no genera excepción no controlada; se registra
  en log `WARN` con stack trace completo.
- **`updateEmailEnviado`**: abre una nueva transacción (REQUIRED sobre contexto vacío del hilo async)
  para marcar `email_enviado = true` tras envío exitoso de aprobación.

---

### `emailEnviado` en `Company`

Campo de auditoría que indica si el email de aprobación fue entregado al servidor SMTP:

```java
@Column(name = "email_enviado", nullable = false)
@Builder.Default
private boolean emailEnviado = false;
```

**Ciclo de vida:**

| Evento | Valor |
|--------|-------|
| Solicitud creada (`PENDIENTE`) | `false` |
| Email enviado tras aprobación | `true` (actualizado por el listener) |
| Email reenviado manualmente | `true` (actualizado por `reenviarEmail()`) |
| Empresa rechazada | — *(borrado físico, no aplica)* |

**SQL de migración** (`docs/sql/add_email_enviado_to_empresa.sql`):

```sql
ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS email_enviado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE empresa
    SET email_enviado = TRUE
    WHERE estado_solicitud = 'APROBADA';
```

> El backfill asume que las empresas ya aprobadas recibieron su email antes de esta migración.

---

### `CompanyRepository.updateEmailEnviado`

```java
@Modifying
@Transactional
@Query("UPDATE Company c SET c.emailEnviado = :enviado WHERE c.empresaId = :id")
void updateEmailEnviado(@Param("id") UUID id, @Param("enviado") boolean enviado);
```

Llamado exclusivamente desde `CompanyEventListener` (aprobación) y `CompanyService.reenviarEmail`.

---

## 3. Endpoint de reenvío manual

### `POST /api/v1/empresas/{id}/reenviar-email`

Permite al superadmin reenviar el email de aprobación cuando `emailEnviado = false`
(fallo SMTP en el envío automático, visible en el listado de empresas).

**Body:**
```json
{ "tipo": "aprobacion" }
```

**Respuestas:**

| Código | Situación |
|--------|-----------|
| `200`  | Email enviado y `email_enviado = true` persistido |
| `400`  | `tipo` inválido o empresa no está en estado `APROBADA` |
| `404`  | Empresa no encontrada |
| `502`  | Fallo SMTP (`GlobalExceptionHandler` captura `MailException`) |

A diferencia del envío automático, aquí `MailException` **no se captura** en el servicio —
se propaga y el `GlobalExceptionHandler` devuelve `502` para que el admin sepa
que el envío falló y pueda reintentar.

---

## 4. Flujo completo por caso de uso

### Aprobación (`PATCH /empresas/{id}/solicitud` con `accion: "aprobar"`)

```
1. resolverSolicitud() → cambiarEstado()
2. Empresa: activo=true, estadoSolicitud=APROBADA, fechaResolucion=now()
3. Usuarios inactivos: activo=true
4. publishEvent(CompanyEvent(PENDIENTE→APROBADA, emailAdmin, nombreAdmin))
5. notificationService.crearInterna() ← síncrono, dentro de la transacción
6. COMMIT
──── hilo async ────────────────────────────────────────────────────────
7. CompanyEventListener.onCompanyEvent()
8. emailService.enviarAprobacion(emailAdmin, nombreAdmin, nombreEmpresa)
9. companyRepository.updateEmailEnviado(empresaId, true)
```

### Rechazo (`PATCH /empresas/{id}/solicitud` con `accion: "rechazar"`)

```
1. resolverSolicitud()
2. Captura: email/nombre del usuario admin (de usuarios con activo=false)
3. publishEvent(CompanyEvent(PENDIENTE→RECHAZADA, emailAdmin, nombreAdmin))
4. userRepository.deleteAllByEmpresaId()   ← FK primero
5. companyRepository.delete(empresa)
6. auditService.registrar(..., "warning")
7. COMMIT
──── hilo async ────────────────────────────────────────────────────────
8. CompanyEventListener.onCompanyEvent()
9. emailService.enviarRechazo(emailAdmin, nombreAdmin, nombreEmpresa)
   (empresa ya no existe en BD — datos viajan en el record inmutable)
```

---

## 5. Diagnóstico de fallos SMTP

Si los emails no llegan, buscar en los logs del servidor entradas con nivel `WARN` del logger
`com.atalayas.backend.company.event.CompanyEventListener`:

```
WARN  CompanyEventListener - Fallo SMTP tras commit — empresa=Acme S.L. estado=APROBADA: ...
```

El mensaje incluye el stack trace completo de la `MailException`. Causas más frecuentes:

| Error | Causa probable | Solución |
|-------|---------------|----------|
| `AuthenticationFailedException` | `MAIL_PASSWORD` no es App Password | Generar App Password en cuenta Google |
| `ConnectException` / `SocketTimeoutException` | Puerto bloqueado o sin conectividad | Verificar entorno Render (puerto 587 debe estar libre) |
| `SMTPSendFailedException: 550` | Dirección "From" no autorizada | `app.mail.from` debe coincidir con `spring.mail.username` |
| `MailSendException: Invalid Addresses` | Email destinatario malformado | Revisar datos del usuario en BD |
