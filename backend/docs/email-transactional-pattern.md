# Email y transacciones — Patrón Maileroo

> **Estado**: ✅ Implementado (Mayo 2026) — usa Maileroo API (cliente HTTP nativo Java 21)
> **Plan**: Free · 3.000 emails/mes · 30 emails/hora · BCC bulk para eventos

---

## Resumen del patrón implementado

Todos los métodos públicos de `EmailService` son `@Async`: fire-and-forget, no bloquean el hilo HTTP.
Los errores se capturan internamente con `try/catch` y se registran en log `ERROR`.

```
HTTP request
    └── Service (transacción activa)
            ├── Cambios en BD
            └── emailService.enviarXxx()   ← @Async: schedula en thread pool y retorna inmediatamente
                    COMMIT

    Después (hilo async del pool):
    EmailService.enviarXxx()
            └── mailerooClient.send() / mailerooClient.sendBcc()
                    └── POST https://api.maileroo.com/send
```

Para las aprobaciones/rechazos de empresa, el `CompanyEventListener` añade una capa extra
de `@TransactionalEventListener(AFTER_COMMIT)` que garantiza que el email solo se intenta
si el commit tuvo éxito:

```
COMMIT
──── hilo async (CompanyEventListener) ────────────────────────────────────
CompanyEventListener.onCompanyEvent()
        ├── emailService.enviarAprobacion()   ← @Async (schedula al pool)
        └── companyRepository.updateEmailEnviado(true)   ← ejecuta inmediatamente
```

---

## 1. Configuración Maileroo API

```properties
# Clave de API obtenida en https://maileroo.com → API Keys
maileroo.api.key=${MAILEROO_API_KEY:}

# (Opcional) Endpoint; por defecto https://api.maileroo.com/send
maileroo.api.url=${MAILEROO_API_URL:https://api.maileroo.com/send}

# Dirección remitente verificada en Maileroo (dominio propio)
app.mail.from=${MAIL_FROM:noreply@atalayas.com}

# URL del frontend — usada en los botones de los emails
app.frontend.url=${FRONTEND_URL:http://localhost:3000}
```

> `MAIL_FROM` debe ser una dirección de un dominio verificado en el panel de Maileroo.

### Cliente HTTP (`MailerooClient`)

No hay SDK externo. Se usa `java.net.http.HttpClient` nativo (Java 21), igual que `GroqClient`
y `GeminiClient`. El cliente vive en `communication/client/MailerooClient.java`.

```java
// Fragmento del método send()
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(apiUrl))                        // maileroo.api.url
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)                    // maileroo.api.key
        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
        .build();
```

Body del request:
```json
{ "from": "...", "to": "...", "subject": "...", "html": "..." }
```

Errores HTTP ≥ 300 o fallos de red → `EmailSendException` → `GlobalExceptionHandler` → HTTP 502.

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

| Método | Tipo | Asunto | Badge | CTA |
|--------|------|--------|-------|-----|
| `enviarAprobacion` | Individual `@Async` | `¡Tu empresa ha sido aprobada en Atalayas!` | 🟢 Verde | Botón → `/login` |
| `enviarRechazo` | Individual `@Async` | `Actualización sobre tu solicitud de alta en Atalayas` | 🔴 Rojo | Link soporte |
| `enviarBienvenidaUsuarioCreado` | Individual `@Async` | `Tu cuenta en Atalayas está lista` | 🔵 Azul | Botón → `/login` |
| `enviarRecuperacionPassword` | Individual `@Async` | `Restablecer contraseña · Atalayas` | — | Botón → `/reset-password?token=…` |
| `enviarNuevoEventoMasivo` | **BCC bulk** `@Async` | `Nuevo evento · {titulo}` | 🟠 Naranja "📅 Nuevo evento" | Botón → `/eventos` |
| `enviarEventoCanceladoMasivo` | **BCC bulk** `@Async` | `Evento cancelado · {titulo}` | ⚫ Gris "❌ Evento cancelado" | — |
| `enviarNuevoEventoComunidadMasivo` | **BCC bulk** `@Async` | `Nuevo evento de comunidad · {titulo}` | 🟣 Morado "🎉 Evento de comunidad" | Botón → `/comunidad` |
| `enviarEventoComunidadDesactivadoMasivo` | **BCC bulk** `@Async` | `Evento desactivado · {titulo}` | ⚫ Gris "❌ Evento desactivado" | — |

### Colores de badge

```
Aprobación              → background: #ECFDF5  color: #059669  (verde)
Rechazo                 → background: #FEF2F2  color: #DC2626  (rojo)
Bienvenida              → background: #EFF6FF  color: #1B3F7E  (azul)
Nuevo evento EGM        → background: #FFF7ED  color: #EA580C  (naranja)
Nuevo evento comunidad  → background: #F5F3FF  color: #7C3AED  (morado)
Cancelado/desactivado   → background: #F3F4F6  color: #6B7280  (gris)
```

### BCC bulk vs. envío individual

Los métodos `*Masivo` usan `mailerooClient.sendBcc()`: una sola llamada HTTP con todos
los destinatarios en el header `bcc`. El campo `to` apunta al remitente para preservar
la privacidad entre destinatarios.

```java
// EmailService — método privado para BCC
private void sendBcc(List<String> bccList, String subject, String html) {
    mailerooClient.sendBcc(remitente, subject, html, bccList);
}
```

Cap de destinatarios: `app.mail.max-bulk-recipients` (default `25`).
Si la lista supera el cap se emite `WARN` y se trunca. Plan Free: 30 emails/hora.

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

Si los emails no llegan, buscar en los logs del servidor:

```
# Fallo tras commit de empresa
WARN  CompanyEventListener - Fallo Maileroo API tras commit — empresa=Acme S.L. estado=APROBADA: ...

# Fallo en método @Async de EmailService
ERROR EmailService - [EmailService] Error al enviar aprobación a admin@empresa.com: ...
ERROR EmailService - [EmailService] Error al enviar nuevo evento masivo 'Jornada': ...

# Cap de destinatarios alcanzado
WARN  EmailService - [EmailService] NuevoEvento[Jornada] — lista de 40 destinatarios truncada a 25 ...
```

Ver también: [`docs/maileroo-api-key-test.md`](maileroo-api-key-test.md) para comandos `curl`/PowerShell,
tabla de errores HTTP y checklist DKIM/SPF.
