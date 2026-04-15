# Tests — EGM Atalayas Backend

> **Audiencia:** Desarrolladores backend
> **Última actualización:** Abril 2026

---

## Índice

1. [Estrategia general](#1-estrategia-general)
2. [Cómo ejecutar los tests](#2-cómo-ejecutar-los-tests)
3. [Base de datos en memoria (H2)](#3-base-de-datos-en-memoria-h2)
4. [CrossCompanySecurityTest](#4-crosscompanysecuritytest)
5. [Cómo añadir nuevos tests](#5-cómo-añadir-nuevos-tests)

---

## 1. Estrategia general

El proyecto tiene **tests de integración** que verifican el comportamiento completo de la cadena HTTP → Spring Security → Controller → Service → Repository, usando MockMvc y una BD H2 en memoria.

El foco principal de los tests actuales es la **seguridad multi-tenant**: garantizar que el aislamiento entre empresas funciona correctamente.

```
src/test/java/com/atalayas/backend/
├── BackendApplicationTests.java       → Test de arranque del contexto
└── CrossCompanySecurityTest.java      → Tests de seguridad multi-tenant (suite principal)

src/test/resources/
└── application-test.properties        → Configuración específica para tests (H2 en memoria)
```

---

## 2. Cómo ejecutar los tests

```bash
# Ejecutar todos los tests
./mvnw test

# En Windows
mvnw.cmd test

# Solo una clase de tests concreta
./mvnw test -Dtest=CrossCompanySecurityTest

# Con output detallado
./mvnw test -Dsurefire.useFile=false
```

Los tests **no necesitan**:
- PostgreSQL arrancado
- Variables de entorno configuradas
- Conexión a internet (Gemini AI no se llama en tests)

---

## 3. Base de datos en memoria (H2)

Para los tests se usa **H2** en lugar de PostgreSQL. Esto permite:
- Ejecutar tests en CI/CD sin infraestructura externa
- Tests rápidos (BD se crea y destruye en memoria)
- Independencia total del entorno

### Configuración (`application-test.properties`)

```properties
# Usar H2 en lugar de PostgreSQL
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver

# Crear el esquema desde cero en cada test
spring.jpa.hibernate.ddl-auto=create-drop

# No enviar correos reales en tests
spring.mail.host=localhost
spring.mail.port=3025
```

El perfil `test` se activa con `@ActiveProfiles("test")` en cada clase de test.

---

## 4. CrossCompanySecurityTest

Este es el test más importante del proyecto. Verifica que el sistema de **aislamiento entre empresas** funciona correctamente en todos los escenarios posibles.

### Configuración inicial (`@BeforeEach`)

Antes de cada test se crea un escenario completo en H2:

```
Empresa A (UUID: ...0001)          Empresa B (UUID: ...0002)
├── empleadoA (ROLE_EMPLEADO)      └── empleadoB (ROLE_EMPLEADO)
└── adminA (ROLE_ADMIN_EMPRESA)

Plataforma (UUID: ...0000)
└── superAdmin (ROLE_ADMIN)
```

### Suites de tests

#### Suite 1: Sin token (`SinToken`)
Verifica que los endpoints protegidos rechazan peticiones sin autenticación.

| Test | Petición | Esperado |
|---|---|---|
| `sinToken_getUserById_retorna401` | `GET /users/{id}` sin token | `401 Unauthorized` |
| `sinToken_getAllUsers_retorna401` | `GET /users` sin token | `401 Unauthorized` |

#### Suite 2: Empleado intenta endpoints de admin (`EmpleadoSinPermisos`)
Verifica que `ROLE_EMPLEADO` no puede acceder a endpoints que requieren `ROLE_ADMIN_EMPRESA` o superior.

| Test | Petición | Esperado |
|---|---|---|
| `empleado_getUserById_retorna403` | `GET /users/{id}` con token de empleado | `403 Forbidden` |
| `empleado_getAllUsers_retorna403` | `GET /users` con token de empleado | `403 Forbidden` |
| `empleado_desactivarUsuario_retorna403` | `DELETE /users/{id}/desactivar` con token de empleado | `403 Forbidden` |

#### Suite 3: Admin de empresa A intenta acceder a empresa B (`AdminCrossCompany`)
Verifica el aislamiento multi-tenant: un admin **no puede** ver datos de otra empresa.

| Test | Petición | Esperado | Razón del comportamiento |
|---|---|---|---|
| `adminA_getUserDeEmpresaB_retorna404` | `GET /users/{idEmpleadoB}` con token adminA | `404 Not Found` | Se devuelve 404 (no 403) para no revelar que el recurso existe |
| `adminA_desactivarUsuarioDeEmpresaB_retorna404` | `DELETE /users/{idEmpleadoB}/desactivar` con token adminA | `404 Not Found` | Mismo principio |
| `adminA_getAllUsers_soloVeSuEmpresa` | `GET /users` con token adminA | `200 OK` + solo 2 usuarios (empresa A) | El listado filtra automáticamente por empresa |

#### Suite 4: Superadmin accede a cualquier empresa (`SuperAdminAccesoTotal`)
Verifica que `ROLE_ADMIN` tiene visibilidad total de la plataforma.

| Test | Petición | Esperado |
|---|---|---|
| `superAdmin_getUserDeEmpresaB_retorna200` | `GET /users/{idEmpleadoB}` con token superAdmin | `200 OK` con datos de empresa B |
| `superAdmin_getAllUsers_veTodas` | `GET /users` con token superAdmin | `200 OK` con los 4 usuarios (todas las empresas) |
| `superAdmin_desactivarUsuarioDeEmpresaB_retorna204` | `DELETE /users/{id}/desactivar` con superAdmin | `204 No Content` |

#### Suite 5: Admin happy path — accede a sus propios datos (`AdminHappyPath`)
Verifica que el admin puede hacer operaciones normales sobre su propia empresa.

| Test | Petición | Esperado |
|---|---|---|
| `adminA_getUserDePropiEmpresa_retorna200` | `GET /users/{idEmpleadoA}` con token adminA | `200 OK` con `empresaId = empresaA` |
| `getMe_retornaDatosDelUsuarioAutenticado` | `GET /users/me` con cualquier token | `200 OK` con los datos del usuario autenticado |

### Cómo se genera el token en los tests

No se hace login HTTP. El token se genera directamente con `JwtService`:

```java
String bearerToken(User user) {
    return "Bearer " + jwtService.generateAccessToken(user);
}

// Uso en el test:
mockMvc.perform(get("/api/v1/users/{id}", empleadoB.getUsuarioId())
        .header("Authorization", bearerToken(adminA)))
        .andExpect(status().isNotFound());
```

---

## 5. Cómo añadir nuevos tests

### Plantilla básica para un nuevo test de integración

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Nombre descriptivo del grupo de tests")
class MiNuevoTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User miUsuario;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role rol = roleRepository.save(Role.builder()
                .nombreRol("Empleado").codigoRol("ROLE_EMPLEADO").build());

        miUsuario = userRepository.save(User.builder()
                .nombre("Test").apellidos("User")
                .email("test@empresa.test")
                .password(passwordEncoder.encode("password123"))
                .empresaId(UUID.randomUUID())
                .rol(rol).build());
    }

    @Test
    @DisplayName("Descripción clara del comportamiento esperado")
    void miTest() throws Exception {
        String token = "Bearer " + jwtService.generateAccessToken(miUsuario);

        mockMvc.perform(get("/api/v1/ruta")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campo").value("valorEsperado"));
    }
}
```

### Buenas prácticas

- Usa `@DisplayName` descriptivo: "GIVEN + WHEN + THEN" o "descripción del comportamiento esperado"
- Limpia la BD en `@BeforeEach` para evitar dependencias entre tests
- Usa `@Nested` para agrupar tests relacionados
- Verifica siempre el código HTTP y al menos un campo del body de respuesta

