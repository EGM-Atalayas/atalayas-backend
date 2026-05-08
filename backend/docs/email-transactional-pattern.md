# Email y transacciones en CompanyService

> **Estado**: ✅ Implementado (Mayo 2026) — usa Resend API (`resend-java:3.1.0`)

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
| Fallo de envío revierte cambio de estado | ✗ (con el fix) | ✓ nunca |
| HTTP response espera al proveedor de email | ✓ bloquea | ✗ async |
| Visibilidad del fallo para el admin | ✗ solo logs | ✓ campo `emailEnviado` |

---

## 1. Configuración Resend API

```properties
# Clave de API obtenida en https://resend.com/api-keys
resend.api.key=${RESEND_API_KEY:re_xxxxxxxxx}

# Dirección remitente verificada en Resend (dominio propio o onboarding@resend.dev para tests)
app.mail.from=${MAIL_FROM:onboarding@resend.dev}

# URL del frontend — usada en los botones de los emails
app.frontend.url=${FRONTEND_URL:http://localhost:3000}
```

> En producción, `MAIL_FROM` debe ser una dirección de un dominio verificado en
> el panel de Resend (Settings → Domains). Usar `onboarding@resend.dev` solo
> permite enviar a la cuenta propia en modo test.

### Bean de configuración

```java
// config/ResendConfig.java
@Configuration
public class ResendConfig {

    @Value("${resend.api.key}")
    private String apiKey;

    @Bean
    public Resend resend() {
        return new Resend(apiKey);
    }
}
```

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
- **`try/catch (Exception)`**: un fallo de Resend API no genera excepción no controlada; se registra
  en log `WARN` con stack trace completo.
- **`updateEmailEnviado`**: abre una nueva transacción (REQUIRED sobre contexto vacío del hilo async)
  para marcar `email_enviado = true` tras envío exitoso de aprobación.

---

### `emailEnviado` en `Company`

Campo de auditoría que indica si el email de aprobación fue entregado correctamente:

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

## 3. Plantillas HTML de email

Todos los emails usan plantillas HTML inline generadas con `String.formatted()`.
El estilo es consistente en todos los mensajes: encabezado azul `#1B3F7E`, tarjeta blanca,
badge de estado coloreado, y footer `© 2026 Atalayas Área Empresarial · Alicante`.

### Plantillas disponibles

| Método | Asunto | Badge | CTA |
|--------|--------|-------|-----|
| `enviarAprobacion` | `¡Tu empresa ha sido aprobada en Atalayas!` | 🟢 Verde — "✓ Solicitud aprobada" | Botón → `{frontendUrl}/login` |
| `enviarRechazo` | `Actualización sobre tu solicitud de alta en Atalayas` | 🔴 Rojo — "✗ Solicitud no aprobada" | Link `soporte@atalayas.com` |
| `enviarBienvenidaUsuarioCreado` | `Tu cuenta en Atalayas está lista` | 🔵 Azul — "Tu cuenta está lista" | Email destacado + Botón → `{frontendUrl}/login` |
| `enviarRecuperacionPassword` | `Restablecer contraseña · Atalayas` | — | Botón → `{frontendUrl}/reset-password?token=…` |

### Colores de badge

```
Aprobación  → background: #ECFDF5  color: #059669  (verde)
Rechazo     → background: #FEF2F2  color: #DC2626  (rojo)
Bienvenida  → background: #EFF6FF  color: #1B3F7E  (azul)
```

### Implementación del envío

```java
// EmailService — método privado común a los 3 emails de empresa
private void send(String to, String subject, String html) {
    try {
        CreateEmailOptions request = CreateEmailOptions.builder()
                .from(remitente)   // app.mail.from
                .to(to)
                .subject(subject)
                .html(html)
                .build();
        resend.emails().send(request);
    } catch (ResendException ex) {
        throw new EmailSendException("Error al enviar email a " + to + ": " + ex.getMessage(), ex);
    }
}
```

`EmailSendException` es capturada por `GlobalExceptionHandler` → devuelve HTTP `502`.

---

## 4. Endpoint de reenvío manual

### `POST /api/v1/empresas/{id}/reenviar-email`

Permite al superadmin reenviar el email de aprobación cuando `emailEnviado = false`
(fallo de Resend API en el envío automático, visible en el listado de empresas).

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
| `502`  | Fallo Resend API (`GlobalExceptionHandler` captura `EmailSendException`) |

A diferencia del envío automático, aquí `EmailSendException` **no se captura** en el servicio —
se propaga y el `GlobalExceptionHandler` devuelve `502` para que el admin sepa
que el envío falló y pueda reintentar.

---

## 5. Flujo completo por caso de uso

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
   └── Resend API → email HTML con badge verde + botón "Acceder a la plataforma"
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
   └── Resend API → email HTML con badge rojo + contacto soporte@atalayas.com
   (empresa ya no existe en BD — datos viajan en el record inmutable)
```

---

## 6. Diagnóstico de fallos

Si los emails no llegan, buscar en los logs del servidor entradas con nivel `WARN` del logger
`com.atalayas.backend.company.event.CompanyEventListener`:

```
WARN  CompanyEventListener - Fallo Resend API tras commit — empresa=Acme S.L. estado=APROBADA: ...
```

### Errores frecuentes de Resend API

| Error | Causa probable | Solución |
|-------|---------------|----------|
| `422 Unprocessable Entity` | Dominio remitente no verificado | Verificar dominio en Resend → Settings → Domains |
| `401 Unauthorized` | `RESEND_API_KEY` inválida o expirada | Regenerar API key en resend.com/api-keys |
| `429 Too Many Requests` | Rate limit superado | Revisar plan de Resend; añadir retry con backoff |
| `ResendException: Invalid 'to' address` | Email destinatario malformado | Revisar datos del usuario en BD |
| Timeout / `ConnectException` | Sin conectividad al endpoint Resend | Verificar red del servidor; `api.resend.com` debe ser accesible |

### Test rápido de la API key

```bash
curl -X POST https://api.resend.com/emails \
  -H "Authorization: Bearer $RESEND_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"from":"onboarding@resend.dev","to":"test@example.com","subject":"Test","text":"OK"}'
```

Respuesta esperada: `{"id":"..."}` con HTTP 200.
