package com.atalayas.backend.department.dto;

import java.util.UUID;

public record DepartamentoResponse(UUID departamentoId, String nombre, boolean esGlobal) {}
