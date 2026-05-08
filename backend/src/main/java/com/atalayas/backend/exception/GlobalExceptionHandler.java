package com.atalayas.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.atalayas.backend.exception.EmailSendException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la aplicación.
 *
 * Convierte cada tipo de excepción en una respuesta HTTP estructurada
 * con timestamp, status, error y message — nunca expone stack traces al cliente.
 *
 * Orden de captura: de más específico a más general.
 * El catch-all de Exception está al final para que cualquier error
 * inesperado devuelva un 500 con estructura en lugar de romperse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 — Recurso no encontrado en base de datos.
     * Se lanza desde los servicios cuando un findById no encuentra resultado.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * 400 — Regla de negocio violada.
     * Ejemplo: intentar aprobar una empresa ya aprobada.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 400 — Estado inválido para la operación solicitada.
     * Ejemplo: intentar desactivar un módulo que ya estaba desactivado.
     * Se lanza con IllegalStateException en los soft deletes de los servicios.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 401 — Usuario autenticado sin permisos sobre el recurso solicitado.
     * Ejemplo: empleado intentando ver módulos de otra empresa.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * 401 — Credenciales de login incorrectas.
     * Spring Security lanza esta excepción cuando email o password no coinciden.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    /**
     * 403 — Acceso denegado por Spring Security.
     * Se lanza cuando un @PreAuthorize falla — el usuario no tiene el rol requerido.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta acción");
    }

    /**
     * 400 — Argumento inválido en una operación.
     * Ejemplo: RoleType.fromCodigo() recibe un código de rol desconocido.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 400 — Validación de campos del request fallida (@Valid en el controller).
     * Devuelve un mapa con cada campo inválido y su mensaje de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validación fallida");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 400 — Jackson no puede deserializar el cuerpo de la petición.
     * Ocurre cuando el JSON contiene un valor de enum inválido (ej: "ACTIVA" en lugar
     * de "APROBADA"), un tipo de dato incorrecto o JSON malformado.
     * Sin este handler la excepción cae al catch-all y devuelve un 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        String message = ex.getCause() != null
                ? ex.getCause().getMessage()
                : "El cuerpo de la petición no es válido o contiene un valor no reconocido";
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 413 — Archivo subido supera el límite configurado (spring.servlet.multipart.max-file-size).
     * Sin este handler Spring cierra la conexión HTTP/2 bruscamente (ERR_HTTP2_PROTOCOL_ERROR).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido (50 MB)");
    }

    /**
     * 404 — Ruta no registrada en la aplicación.
     * Evita que una URL inexistente caiga en el catch-all y devuelva un 500.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            NoHandlerFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND,
                "Endpoint no encontrado: " + ex.getHttpMethod() + " " + ex.getRequestURL());
    }

    /**
     * 502 — Fallo al enviar correo electrónico (dependencia SMTP externa).
     * HTTP 502 Bad Gateway es semánticamente correcto: el servidor actuó como
     * proxy hacia Gmail/SMTP y recibió una respuesta inválida o no recibió
     * respuesta en el tiempo esperado.
     *
     * Nota: CompanyService ya captura MailException localmente para que el
     * fallo SMTP no revierta la transacción de BD. Este handler cubre cualquier
     * otro punto del sistema donde pueda escapar sin capturar.
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, Object>> handleEmailSend(EmailSendException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY,
                "No se pudo enviar el correo electrónico. Inténtalo de nuevo en unos minutos.");
    }

    /**
     * 500 — Cualquier excepción no controlada que llegue hasta aquí.
     * No expone el mensaje original para no filtrar información interna al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }


    // ── BUILDER DE RESPUESTA ESTRUCTURADA ─────────────────────────────────

    /**
     * Construye el cuerpo de respuesta de error estándar de la plataforma.
     * Todos los errores tienen la misma estructura para que el frontend
     * pueda procesarlos de forma uniforme sin casos especiales.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}