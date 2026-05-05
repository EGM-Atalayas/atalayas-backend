# Email y transacciones en CompanyService

## 1. Config SMTP: puerto 465 (SSL) vs 587 (STARTTLS)

Los dos puertos de Gmail usan protocolos distintos e **incompatibles**:

| Puerto | Protocolo | Propiedades Spring Mail |
|--------|-----------|-------------------------|
| **465** | SMTPS — SSL/TLS directo desde el inicio | `mail.smtp.ssl.enable=true` |
| **587** | PLAIN → upgrade a TLS con STARTTLS | `mail.smtp.starttls.enable=true` + `mail.smtp.starttls.required=true` |

### Problema original

`application.properties` tenía puerto `465` pero con `starttls.*` activo.
Al conectar, el cliente intentaba un handshake STARTTLS sobre un socket que
ya esperaba TLS nativo → el servidor cerraba la conexión sin responder.
`JavaMailSender.send()` quedaría bloqueado hasta el timeout del SO (~136 s
observados en los logs de Render), momento en que lanzaba `MailException`.

### Config correcta para port 465 (aplicada)

```properties
spring.mail.port=${MAIL_PORT:465}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
```

Los timeouts de 10 s garantizan que incluso ante un fallo total de conectividad
SMTP el hilo de request se libera en ≤ 10 s en lugar de ≥ 136 s.

---

## 2. Solución aplicada: try-catch + timeouts

`CompanyService.cambiarEstado()` y `resolverSolicitud()` son métodos
`@Transactional`. Antes del fix, `MailException` al enviar el email:

1. Salía de `cambiarEstado()` como excepción no capturada.
2. Marcaba la transacción para rollback → el cambio de estado en BD
   **nunca se persistía**.
3. Llegaba al `handleGeneral(Exception ex)` del `GlobalExceptionHandler`
   → HTTP 500 "Error interno del servidor".

### Fix aplicado

Cada llamada a `emailService.enviar*()` está ahora envuelta en:

```java
try {
    emailService.enviarAprobacion(email, nombre, nombreEmpresa);
} catch (MailException ex) {
    log.warn("No se pudo enviar email de aprobación a {} — empresa={}: {}",
            email, nombreEmpresa, ex.getMessage());
}
```

**Ventajas:**
- El cambio de estado en BD se persiste aunque el email falle.
- La respuesta HTTP es 200 en lugar de 500.
- El fallo queda trazado en logs con `WARN`.

**Limitación:**
- El fallo es silencioso para el admin: la UI muestra éxito aunque el email
  no se haya enviado. Se mitiga con el endpoint de reenvío manual (sección 4).

Adicionalmente, `GlobalExceptionHandler` tiene ahora un handler explícito
para `MailException` → HTTP 502, que cubre cualquier otro punto del sistema
donde pudiera escapar sin capturar.

---

## 3. Alternativa robusta: @TransactionalEventListener

Esta es la solución estructuralmente correcta. El email **no debe enviarse
hasta que el commit de BD sea definitivo** — si el commit falla por cualquier
motivo, el email nunca debe salir.

### Piezas necesarias

**`CompanyEvent.java`** (record, en `company/event/`):

```java
package com.atalayas.backend.company.event;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import java.util.UUID;

/**
 * Evento publicado por CompanyService tras persistir un cambio de estado.
 * Se procesa en CompanyEventListener DESPUÉS del commit de BD.
 */
public record CompanyEvent(
        UUID empresaId,
        String nombreEmpresa,
        String emailAdmin,
        String nombreAdmin,
        EstadoSolicitud estadoAnterior,
        EstadoSolicitud estadoNuevo
) {}
```

**`CompanyEventListener.java`** (en `company/event/`):

```java
package com.atalayas.backend.company.event;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.communication.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha CompanyEvent y envía el email correspondiente DESPUÉS del commit.
 *
 * Al usar phase = AFTER_COMMIT:
 *   - El email solo se envía si la transacción de BD completó con éxito.
 *   - Un fallo SMTP no puede revertir nada porque el commit ya ocurrió.
 *   - No es necesario try-catch ad-hoc en CompanyService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyEvent(CompanyEvent event) {
        try {
            if (event.estadoAnterior() == EstadoSolicitud.PENDIENTE
                    && event.estadoNuevo() == EstadoSolicitud.APROBADA) {
                emailService.enviarAprobacion(
                        event.emailAdmin(), event.nombreAdmin(), event.nombreEmpresa());
            }
            // Añadir más casos según necesidad (rechazo, pausa, etc.)
        } catch (MailException ex) {
            log.warn("Fallo SMTP tras commit — empresa={} tipo={}: {}",
                    event.nombreEmpresa(), event.estadoNuevo(), ex.getMessage());
        }
    }
}
```

**Cambio en `CompanyService.cambiarEstado()`** — sustituir la llamada directa
al email por la publicación del evento:

```java
// Añadir al constructor / @RequiredArgsConstructor:
private final ApplicationEventPublisher eventPublisher;

// Al final de cambiarEstado(), antes del return:
eventPublisher.publishEvent(new CompanyEvent(
        company.getEmpresaId(),
        company.getNombreEmpresa(),
        adminUser.getEmail(),   // obtener el admin de la lista afectados
        adminUser.getNombre(),
        actual,
        destino
));
```

### Por qué es superior al try-catch

| | try-catch (aplicado) | @TransactionalEventListener |
|---|---|---|
| Email se envía si el commit falla | ✗ posible | ✓ nunca |
| Email se envía si el commit tiene éxito | ✓ | ✓ |
| Código limpio en el servicio | ✗ ruido | ✓ |
| Testeable de forma independiente | ✗ acoplado | ✓ |

---

## 4. Endpoint de reenvío manual (TO-DO)

Permite al superadmin reenviar el email de aprobación cuando el envío
automático falló silenciosamente.

### Contrato HTTP

```
POST /api/v1/empresas/{id}/reenviar-email
Authorization: Bearer <token ROLE_ADMIN>
Content-Type: application/json

{ "tipo": "aprobacion" }   -- único valor soportado en v1
```

**Respuestas:**

| Código | Situación |
|--------|-----------|
| 200    | Email enviado y `email_enviado` marcado a `true` |
| 400    | `tipo` inválido o empresa no está en estado `APROBADA` |
| 404    | Empresa no encontrada |
| 502    | Fallo SMTP (capturado por `GlobalExceptionHandler`) |

### Flujo de servicio (`CompanyService.reenviarEmail`)

```java
@Transactional
public void reenviarEmail(UUID id, String tipo) {
    Company empresa = findOrThrow(id);

    if (empresa.getEstadoSolicitud() != EstadoSolicitud.APROBADA) {
        throw new BusinessException(
            "Solo se puede reenviar email a empresas en estado APROBADA");
    }

    User admin = userRepository.findAllByEmpresaId(id).stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontró usuario admin para la empresa: " + id));

    // Lanza MailException si falla — el GlobalExceptionHandler devuelve 502
    if ("aprobacion".equalsIgnoreCase(tipo)) {
        emailService.enviarAprobacion(
                admin.getEmail(), admin.getNombre(), empresa.getNombreEmpresa());
    } else {
        throw new BusinessException("Tipo de email no reconocido: " + tipo);
    }

    empresa.setEmailEnviado(true);
    companyRepository.save(empresa);

    auditService.registrar(
        "Email de " + tipo + " reenviado a empresa \"" + empresa.getNombreEmpresa() + "\"",
        "info");
}
```

### Controller

```java
@PostMapping("/{id}/reenviar-email")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Operation(summary = "Reenviar email a empresa (SUPER_ADMIN)",
           description = "Reenvía el email de aprobación cuando el envío automático falló. " +
                         "Actualiza `email_enviado = true` si tiene éxito.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Email enviado correctamente"),
    @ApiResponse(responseCode = "400", description = "Tipo inválido o empresa no APROBADA"),
    @ApiResponse(responseCode = "404", description = "Empresa no encontrada"),
    @ApiResponse(responseCode = "502", description = "Fallo SMTP")
})
public ResponseEntity<Void> reenviarEmail(
        @PathVariable UUID id,
        @Valid @RequestBody ReenviarEmailRequest request) {
    companyService.reenviarEmail(id, request.getTipo());
    return ResponseEntity.ok().build();
}
```

**`ReenviarEmailRequest.java`:**

```java
@Data
public class ReenviarEmailRequest {
    @NotBlank
    @Pattern(regexp = "aprobacion", message = "Tipo debe ser 'aprobacion'")
    private String tipo;
}
```

### Cambios en entidad y migración SQL

Requiere la columna `email_enviado` en la tabla `empresa`.
Ver migración completa en [`docs/sql/add_email_enviado_to_empresa.sql`](sql/add_email_enviado_to_empresa.sql).

**`Company.java`** — añadir campo:

```java
@Column(name = "email_enviado", nullable = false)
@Builder.Default
private boolean emailEnviado = false;
```

**`CompanyResponse`** — añadir campo `boolean emailEnviado` para que el
frontend pueda mostrar un badge de advertencia cuando sea `false` en una
empresa aprobada.

**`CompanyMapper.toResponse()`** — añadir `.emailEnviado(company.isEmailEnviado())`.

**`CompanyService.cambiarEstado()`** — en el bloque `APROBADA`, tras el save:

```java
company.setEmailEnviado(emailEnviado);  // true si el try-catch no capturó nada
```

O mejor aún, al migrar a `@TransactionalEventListener`, actualizar
`emailEnviado` dentro del listener tras el envío exitoso mediante un
`CompanyRepository.updateEmailEnviado(empresaId, true)` con `@Modifying`.

