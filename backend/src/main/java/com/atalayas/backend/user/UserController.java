package com.atalayas.backend.user;

import com.atalayas.backend.common.dto.PaginatedResponse;
import com.atalayas.backend.user.dto.AdminResetPasswordRequest;
import com.atalayas.backend.user.dto.ChangePasswordRequest;
import com.atalayas.backend.user.dto.CreateUserRequest;
import com.atalayas.backend.user.dto.UpdateProfileRequest;
import com.atalayas.backend.user.dto.UpdateUserRequest;
import com.atalayas.backend.user.dto.UserProfileResponse;
import com.atalayas.backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(
        summary = "Crear usuario (solo admins)",
        description = """
            Crea un nuevo usuario activo en la plataforma.

            - **ROLE_ADMIN_EMPRESA**: crea usuarios solo en su propia empresa. \
            No puede asignar el rol ROLE_ADMIN. El campo `empresaId` se ignora.
            - **ROLE_ADMIN**: puede crear usuarios en cualquier empresa \
            (campo `empresaId` obligatorio). Puede asignar cualquier rol.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado correctamente",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email ya registrado, datos inválidos o empresaId ausente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Sin permisos para asignar ese rol",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<UserResponse> crearUsuario(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.crearUsuario(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil del usuario autenticado")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PatchMapping("/me")
    @Operation(summary = "Actualizar perfil propio")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    @PostMapping("/me/password")
    @Operation(summary = "Cambiar contraseña propia")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeMyPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir avatar del usuario autenticado")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = userService.uploadAvatar(file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Obtener usuario por ID (solo admins)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Actualizar datos de un usuario (solo admins)")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Listar todos los usuarios (solo admins)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Listar usuarios paginados con búsqueda opcional (solo admins)")
    public ResponseEntity<PaginatedResponse<UserResponse>> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getAllUsersPaged(page, size, search));
    }

    @DeleteMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Desactivar usuario (solo admins)")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable UUID id) {
        userService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Activar usuario (solo admins)")
    public ResponseEntity<Void> activarUsuario(@PathVariable UUID id) {
        userService.activarUsuario(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")
    @Operation(summary = "Resetear contraseña de un empleado (solo admins)")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
