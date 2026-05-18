package com.atalayas.backend.department;

import com.atalayas.backend.department.dto.DepartamentoResponse;
import com.atalayas.backend.department.service.DepartamentoService;
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

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<List<DepartamentoResponse>> listarParaEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(departamentoService.listarParaEmpresa(empresaId));
    }
}
