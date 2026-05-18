package com.atalayas.backend.usuario.departamento;

import com.atalayas.backend.usuario.departamento.dto.DepartamentoResponse;
import com.atalayas.backend.usuario.departamento.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departamentos")
@RequiredArgsConstructor
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    /** Lista los departamentos disponibles para una empresa (globales + propios) */
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPRESA_ADMIN')")
    public ResponseEntity<List<DepartamentoResponse>> listarParaEmpresa(
            @PathVariable UUID empresaId) {
        return ResponseEntity.ok(departamentoService.listarParaEmpresa(empresaId));
    }
}
