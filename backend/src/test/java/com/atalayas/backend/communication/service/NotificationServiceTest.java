package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.NotificationRequest;
import com.atalayas.backend.communication.dto.NotificationResponse;
import com.atalayas.backend.communication.entity.Notification;
import com.atalayas.backend.communication.mapper.NotificationMapper;
import com.atalayas.backend.communication.repository.NotificationRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Spy
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private UUID userId;
    private Notification notification;
    private UUID notifId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notifId = UUID.randomUUID();

        Rol role = Rol.builder()
                .rolId(UUID.randomUUID())
                .codigoRol("ROLE_ADMIN")
                .nombreRol("Administrador")
                .build();

        user = User.builder()
                .usuarioId(userId)
                .email("admin@test.com")
                .password("hash")
                .nombre("Admin")
                .apellidos("Test")
                .rol(role)
                .build();

        notification = Notification.builder()
                .notificacionId(notifId)
                .destinatarioId(userId)
                .tipo("ANUNCIO")
                .mensaje("Test mensaje")
                .leido(false)
                .build();
    }

    // ── crear ─────────────────────────────────────────────────────────────────

    @Test
    void crear_requestValido_devuelveResponse() {
        NotificationRequest req = NotificationRequest.builder()
                .destinatarioId(userId)
                .tipo("ANUNCIO")
                .mensaje("Hola")
                .build();

        when(notificationRepository.save(any())).thenReturn(notification);

        NotificationResponse resp = notificationService.crear(req, user);

        assertThat(resp).isNotNull();
        assertThat(resp.getDestinatarioId()).isEqualTo(userId);
        verify(notificationRepository).save(any(Notification.class));
    }

    // ── crearInterna ──────────────────────────────────────────────────────────

    @Test
    void crearInterna_parametrosValidos_guardaYDevuelve() {
        when(notificationRepository.save(any())).thenReturn(notification);

        NotificationResponse resp = notificationService.crearInterna(
                userId, "BIENVENIDA", "Bienvenido", null);

        assertThat(resp.getTipo()).isEqualTo("ANUNCIO"); // mapper mapea lo que devuelve el mock
        verify(notificationRepository).save(any(Notification.class));
    }

    // ── listarMiasPaginado ────────────────────────────────────────────────────

    @Test
    void listarMiasPaginado_usuarioConNotifs_devuelvePagina() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByDestinatarioIdOrderByCreadoEnDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.listarMiasPaginado(user, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarMiasPaginado_sinNotifs_devuelvePaginaVacia() {
        when(notificationRepository.findByDestinatarioIdOrderByCreadoEnDesc(eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<NotificationResponse> result = notificationService.listarMiasPaginado(user, 0, 20);

        assertThat(result.isEmpty()).isTrue();
    }

    // ── listarMisNoLeidas ─────────────────────────────────────────────────────

    @Test
    void listarMisNoLeidas_conNoLeidas_devuelveLista() {
        when(notificationRepository.findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(userId))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.listarMisNoLeidas(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLeido()).isFalse();
    }

    @Test
    void listarMisNoLeidas_sinNoLeidas_devuelveListaVacia() {
        when(notificationRepository.findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(userId))
                .thenReturn(List.of());

        assertThat(notificationService.listarMisNoLeidas(user)).isEmpty();
    }

    // ── contarNoLeidas ────────────────────────────────────────────────────────

    @Test
    void contarNoLeidas_devuelveContador() {
        when(notificationRepository.countByDestinatarioIdAndLeidoFalse(userId)).thenReturn(3L);

        assertThat(notificationService.contarNoLeidas(user)).isEqualTo(3L);
    }

    // ── marcarComoLeida ───────────────────────────────────────────────────────

    @Test
    void marcarComoLeida_notifExisteSinLeer_marcaYDevuelve() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        Notification leida = Notification.builder()
                .notificacionId(notifId)
                .destinatarioId(userId)
                .tipo("ANUNCIO")
                .mensaje("Test mensaje")
                .leido(true)
                .build();
        when(notificationRepository.save(any())).thenReturn(leida);

        NotificationResponse resp = notificationService.marcarComoLeida(notifId, user);

        assertThat(resp.isLeido()).isTrue();
    }

    @Test
    void marcarComoLeida_notificacionNoExiste_lanza404() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.marcarComoLeida(notifId, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarComoLeida_otroDestinatario_lanza400() {
        Notification ajena = Notification.builder()
                .notificacionId(notifId)
                .destinatarioId(UUID.randomUUID())
                .tipo("ANUNCIO")
                .mensaje("Ajena")
                .leido(false)
                .build();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> notificationService.marcarComoLeida(notifId, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void marcarComoLeida_yaLeida_lanza400() {
        Notification leida = Notification.builder()
                .notificacionId(notifId)
                .destinatarioId(userId)
                .tipo("ANUNCIO")
                .mensaje("Leída")
                .leido(true)
                .build();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(leida));

        assertThatThrownBy(() -> notificationService.marcarComoLeida(notifId, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("leída");
    }

    // ── marcarTodasComoLeidas ─────────────────────────────────────────────────

    @Test
    void marcarTodasComoLeidas_devuelveNumeroDActualizadas() {
        when(notificationRepository.marcarTodasComoLeidas(userId)).thenReturn(5);

        assertThat(notificationService.marcarTodasComoLeidas(user)).isEqualTo(5);
    }

    @Test
    void marcarTodasComoLeidas_sinPendientes_devuelveCero() {
        when(notificationRepository.marcarTodasComoLeidas(userId)).thenReturn(0);

        assertThat(notificationService.marcarTodasComoLeidas(user)).isEqualTo(0);
    }
}

