package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.entity.Notification;
import com.atalayas.backend.communication.repository.NotificationRepository;
import com.atalayas.backend.role.entity.Role;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.security.JwtService;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    private User admin;
    private User empleado;
    private String tokenAdmin;
    private String tokenEmpleado;

    @BeforeEach
    void setUp() {
        Role roleAdmin = roleRepository.save(Role.builder()
                .codigoRol("ROLE_ADMIN").nombreRol("Administrador").build());
        Role roleEmpleado = roleRepository.save(Role.builder()
                .codigoRol("ROLE_EMPLEADO").nombreRol("Empleado").build());

        admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("pass"))
                .nombre("Admin").apellidos("Test")
                .rol(roleAdmin).build());

        empleado = userRepository.save(User.builder()
                .email("empleado@test.com")
                .password(passwordEncoder.encode("pass"))
                .nombre("Empleado").apellidos("Test")
                .rol(roleEmpleado).build());

        tokenAdmin = jwtService.generateAccessToken(admin);
        tokenEmpleado = jwtService.generateAccessToken(empleado);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    // POST /api/v1/notificaciones

    @Test
    void crear_adminValido_devuelve201() throws Exception {
        Map<String, Object> body = Map.of(
                "destinatarioId", empleado.getUsuarioId().toString(),
                "tipo", "ANUNCIO",
                "mensaje", "Mensaje de prueba");

        mockMvc.perform(post("/api/v1/notificaciones")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destinatarioId").value(empleado.getUsuarioId().toString()))
                .andExpect(jsonPath("$.leido").value(false));
    }

    @Test
    void crear_empleado_devuelve403() throws Exception {
        Map<String, Object> body = Map.of(
                "destinatarioId", admin.getUsuarioId().toString(),
                "tipo", "ANUNCIO",
                "mensaje", "No deberia poder");

        mockMvc.perform(post("/api/v1/notificaciones")
                        .header("Authorization", "Bearer " + tokenEmpleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_bodyInvalido_devuelve400() throws Exception {
        Map<String, Object> body = Map.of("tipo", "ANUNCIO", "mensaje", "Sin destinatario");

        mockMvc.perform(post("/api/v1/notificaciones")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // GET /me

    @Test
    void listarMias_conNotifs_devuelvePaginaConContent() throws Exception {
        notif(empleado.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), true);

        mockMvc.perform(get("/api/v1/notificaciones/me")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listarMias_paginacion_respetaSize() throws Exception {
        for (int i = 0; i < 5; i++) notif(empleado.getUsuarioId(), false);

        mockMvc.perform(get("/api/v1/notificaciones/me?page=0&size=3")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    // GET /me/no-leidas

    @Test
    void noLeidas_soloDevuelveNoLeidas() throws Exception {
        notif(empleado.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), true);

        mockMvc.perform(get("/api/v1/notificaciones/me/no-leidas")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].leido").value(false));
    }

    // GET /me/contador

    @Test
    void contador_devuelveNumeroCorrectoDeNoLeidas() throws Exception {
        notif(empleado.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), true);

        mockMvc.perform(get("/api/v1/notificaciones/me/contador")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(2));
    }

    @Test
    void contador_sinNotifs_devuelveCero() throws Exception {
        mockMvc.perform(get("/api/v1/notificaciones/me/contador")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(0));
    }

    // PATCH /{id}/leer

    @Test
    void marcarComoLeida_propiaNoLeida_devuelve200() throws Exception {
        Notification n = notif(empleado.getUsuarioId(), false);

        mockMvc.perform(patch("/api/v1/notificaciones/{id}/leer", n.getNotificacionId())
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leido").value(true));
    }

    @Test
    void marcarComoLeida_yaLeida_devuelve400() throws Exception {
        Notification n = notif(empleado.getUsuarioId(), true);

        mockMvc.perform(patch("/api/v1/notificaciones/{id}/leer", n.getNotificacionId())
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("leída")));
    }

    @Test
    void marcarComoLeida_otroUsuario_devuelve400() throws Exception {
        Notification n = notif(admin.getUsuarioId(), false);

        mockMvc.perform(patch("/api/v1/notificaciones/{id}/leer", n.getNotificacionId())
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("permiso")));
    }

    @Test
    void marcarComoLeida_idInexistente_devuelve404() throws Exception {
        mockMvc.perform(patch("/api/v1/notificaciones/{id}/leer", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isNotFound());
    }

    // PATCH /me/leer-todas

    @Test
    void marcarTodasComoLeidas_conPendientes_devuelveActualizadas() throws Exception {
        notif(empleado.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), false);

        mockMvc.perform(patch("/api/v1/notificaciones/me/leer-todas")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(2));
    }

    @Test
    void marcarTodasComoLeidas_sinPendientes_devuelveCero() throws Exception {
        notif(empleado.getUsuarioId(), true);

        mockMvc.perform(patch("/api/v1/notificaciones/me/leer-todas")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(0));
    }

    @Test
    void marcarTodasComoLeidas_soloAfectaAlUsuarioAutenticado() throws Exception {
        notif(admin.getUsuarioId(), false);
        notif(empleado.getUsuarioId(), false);

        mockMvc.perform(patch("/api/v1/notificaciones/me/leer-todas")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(1));
    }

    // Helper

    private Notification notif(UUID destinatarioId, boolean leido) {
        return notificationRepository.save(Notification.builder()
                .destinatarioId(destinatarioId)
                .tipo("ANUNCIO")
                .mensaje("Mensaje de prueba")
                .leido(leido)
                .build());
    }
}

