package com.atalayas.backend;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.security.JwtService;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Tests de aislamiento entre empresas (multi-tenant security).
 *
 * Verifica que:
 *  - Un ADMIN/EMPLEADO de la empresa A no puede acceder a datos de la empresa B.
 *  - El acceso a recursos de otra empresa devuelve 404 (enmascarado), no 403.
 *  - SUPER_ADMIN puede acceder a recursos de cualquier empresa.
 *  - Peticiones sin token reciben 401.
 *  - EMPLEADO recibe 403 al intentar usar endpoints solo para admins.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Aislamiento entre empresas (multi-tenant)")
class CrossCompanySecurityTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    static final UUID EMPRESA_A        = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EMPRESA_B        = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EMPRESA_PLATFORM = UUID.fromString("00000000-0000-0000-0000-000000000000");
    User empleadoA;
    User adminA;
    User empleadoB;
    User superAdmin;
    // ── Setup ────────────────────────────────────────────────────────────────
    @BeforeEach
    void setUp() {
        // Limpiar en orden correcto: usuario tiene FK a rol
        userRepository.deleteAll();
        roleRepository.deleteAll();
        Rol rolEmpleado = roleRepository.save(Rol.builder()
                .nombreRol("Empleado").codigoRol("EMPLEADO").build());
        Rol rolAdmin = roleRepository.save(Rol.builder()
                .nombreRol("Administrador").codigoRol("ADMIN").build());
        Rol rolSuperAdmin = roleRepository.save(Rol.builder()
                .nombreRol("Super Administrador").codigoRol("SUPER_ADMIN").build());
        empleadoA = userRepository.save(User.builder()
                .nombre("Empleado").apellidos("EmpresaA")
                .email("empleado@empresaa.test")
                .password(passwordEncoder.encode("password123"))
                .empresaId(EMPRESA_A).rol(rolEmpleado).build());
        adminA = userRepository.save(User.builder()
                .nombre("Admin").apellidos("EmpresaA")
                .email("admin@empresaa.test")
                .password(passwordEncoder.encode("password123"))
                .empresaId(EMPRESA_A).rol(rolAdmin).build());
        empleadoB = userRepository.save(User.builder()
                .nombre("Empleado").apellidos("EmpresaB")
                .email("empleado@empresab.test")
                .password(passwordEncoder.encode("password123"))
                .empresaId(EMPRESA_B).rol(rolEmpleado).build());
        superAdmin = userRepository.save(User.builder()
                .nombre("Super").apellidos("Admin")
                .email("superadmin@platform.test")
                .password(passwordEncoder.encode("password123"))
                .empresaId(EMPRESA_PLATFORM).rol(rolSuperAdmin).build());
    }
    /** Genera la cabecera Authorization: Bearer <token> para el usuario dado. */
    String bearerToken(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }
    // ── Sin token ────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Sin token de autenticacion")
    class SinToken {
        @Test
        @DisplayName("GET /users/{id} sin token -> 401 Unauthorized")
        void sinToken_getUserById_retorna401() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", empleadoA.getUsuarioId()))
                    .andExpect(status().isUnauthorized());
        }
        @Test
        @DisplayName("GET /users sin token -> 401 Unauthorized")
        void sinToken_getAllUsers_retorna401() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());
        }
    }
    // ── EMPLEADO intenta endpoints de admin ──────────────────────────────────
    @Nested
    @DisplayName("EMPLEADO accede a endpoints solo para ADMIN/SUPER_ADMIN")
    class EmpleadoSinPermisos {
        @Test
        @DisplayName("GET /users/{id} con EMPLEADO -> 403 Forbidden")
        void empleado_getUserById_retorna403() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(empleadoA)))
                    .andExpect(status().isForbidden());
        }
        @Test
        @DisplayName("GET /users con EMPLEADO -> 403 Forbidden")
        void empleado_getAllUsers_retorna403() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", bearerToken(empleadoA)))
                    .andExpect(status().isForbidden());
        }
        @Test
        @DisplayName("DELETE /users/{id}/desactivar con EMPLEADO -> 403 Forbidden")
        void empleado_desactivarUsuario_retorna403() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}/desactivar", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(empleadoA)))
                    .andExpect(status().isForbidden());
        }
    }
    // ── ADMIN empresa A accede a empresa B (404 enmascarado) ─────────────────
    @Nested
    @DisplayName("ADMIN empresa A intenta acceder a datos de empresa B")
    class AdminCrossCompany {
        @Test
        @DisplayName("GET /users/{idEmpresaB} con ADMIN empresa A -> 404 enmascarado")
        void adminA_getUserDeEmpresaB_retorna404() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(adminA)))
                    .andExpect(status().isNotFound());
        }
        @Test
        @DisplayName("DELETE /users/{idEmpresaB}/desactivar con ADMIN empresa A -> 404 enmascarado")
        void adminA_desactivarUsuarioDeEmpresaB_retorna404() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}/desactivar", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(adminA)))
                    .andExpect(status().isNotFound());
        }
        @Test
        @DisplayName("GET /users con ADMIN empresa A -> solo lista usuarios de empresa A")
        void adminA_getAllUsers_soloVeSuEmpresa() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", bearerToken(adminA))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // empleadoA + adminA = 2 (empresa B no aparece)
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].empresaId",
                            everyItem(is(EMPRESA_A.toString()))));
        }
    }
    // ── SUPER_ADMIN accede a cualquier empresa ────────────────────────────────
    @Nested
    @DisplayName("SUPER_ADMIN accede a datos de cualquier empresa")
    class SuperAdminAccesoTotal {
        @Test
        @DisplayName("GET /users/{idEmpresaB} con SUPER_ADMIN -> 200 OK")
        void superAdmin_getUserDeEmpresaB_retorna200() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(superAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuarioId").value(empleadoB.getUsuarioId().toString()))
                    .andExpect(jsonPath("$.empresaId").value(EMPRESA_B.toString()));
        }
        @Test
        @DisplayName("GET /users con SUPER_ADMIN -> lista usuarios de todas las empresas")
        void superAdmin_getAllUsers_veTodas() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", bearerToken(superAdmin))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // empleadoA + adminA + empleadoB + superAdmin = 4
                    .andExpect(jsonPath("$", hasSize(4)));
        }
        @Test
        @DisplayName("DELETE /users/{idEmpresaB}/desactivar con SUPER_ADMIN -> 204 No Content")
        void superAdmin_desactivarUsuarioDeEmpresaB_retorna204() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}/desactivar", empleadoB.getUsuarioId())
                            .header("Authorization", bearerToken(superAdmin)))
                    .andExpect(status().isNoContent());
        }
    }
    // ── ADMIN happy path: accede a sus propios datos ──────────────────────────
    @Nested
    @DisplayName("ADMIN empresa A accede a sus propios datos (happy path)")
    class AdminHappyPath {
        @Test
        @DisplayName("GET /users/{idEmpresaA} con ADMIN empresa A -> 200 OK")
        void adminA_getUserDePropiEmpresa_retorna200() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", empleadoA.getUsuarioId())
                            .header("Authorization", bearerToken(adminA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.empresaId").value(EMPRESA_A.toString()));
        }
        @Test
        @DisplayName("GET /users/me con cualquier usuario autenticado -> 200 con sus datos")
        void getMe_retornaDatosDelUsuarioAutenticado() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", bearerToken(empleadoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(empleadoA.getEmail()))
                    .andExpect(jsonPath("$.empresaId").value(EMPRESA_A.toString()));
        }
    }
}
