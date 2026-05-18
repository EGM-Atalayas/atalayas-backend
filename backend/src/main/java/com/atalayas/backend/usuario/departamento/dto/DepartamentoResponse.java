package com.atalayas.backend.usuario.departamento.dto;

import java.util.UUID;

public record DepartamentoResponse(
        UUID departamentoId,
        String nombre,
        boolean esGlobal
) {}
