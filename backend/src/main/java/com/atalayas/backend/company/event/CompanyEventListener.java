package com.atalayas.backend.company.event;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.communication.service.EmailService;
import com.atalayas.backend.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha CompanyEvent y envía el email correspondiente DESPUÉS del commit de BD.
 *
 * - phase = AFTER_COMMIT garantiza que el email solo sale si la transacción tuvo éxito.
 * - @Async descarga el envío SMTP a un hilo del pool: la respuesta HTTP al admin
 *   no espera al servidor de correo (mejora latencia percibida).
 * - Un fallo SMTP no puede causar rollback porque el commit ya ocurrió.
 * - updateEmailEnviado() abre una nueva transacción (REQUIRED sobre contexto vacío)
 *   solo en el caso de aprobación; el rechazo elimina la empresa físicamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventListener {

    private final EmailService emailService;
    private final CompanyRepository companyRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyEvent(CompanyEvent event) {
        try {
            if (event.estadoAnterior() == EstadoSolicitud.PENDIENTE
                    && event.estadoNuevo() == EstadoSolicitud.APROBADA) {

                emailService.enviarAprobacion(
                        event.emailAdmin(), event.nombreAdmin(), event.nombreEmpresa());

                // Marcar email enviado — la empresa existe (no fue borrada)
                companyRepository.updateEmailEnviado(event.empresaId(), true);

                log.info("Email de aprobación enviado a {} — empresa={}",
                        event.emailAdmin(), event.nombreEmpresa());

            } else if (event.estadoNuevo() == EstadoSolicitud.RECHAZADA) {

                emailService.enviarRechazo(
                        event.emailAdmin(), event.nombreAdmin(), event.nombreEmpresa());

                // La empresa ya fue eliminada: no hay emailEnviado que actualizar

                log.info("Email de rechazo enviado a {} — empresa={}",
                        event.emailAdmin(), event.nombreEmpresa());
            }
        } catch (MailException ex) {
            log.warn("Fallo SMTP tras commit — empresa={} estado={}: {}",
                    event.nombreEmpresa(), event.estadoNuevo(), ex.getMessage(), ex);
        }
    }
}

