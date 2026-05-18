package com.atalayas.backend.department.service;

import com.atalayas.backend.department.dto.DepartamentoResponse;
import com.atalayas.backend.department.entity.Departamento;
import com.atalayas.backend.department.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;

    public List<DepartamentoResponse> listarParaEmpresa(UUID empresaId) {
        return departamentoRepository
                .findByEmpresaIdIsNullAndActivoTrueOrEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(d -> new DepartamentoResponse(
                        d.getDepartamentoId(),
                        d.getNombre(),
                        d.getEmpresaId() == null
                ))
                .toList();
    }
}
