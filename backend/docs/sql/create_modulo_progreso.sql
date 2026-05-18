-- =============================================================
-- Tabla: modulo_progreso
-- Registra el progreso de cada empleado por módulo formativo.
--
-- A diferencia de 'trazabilidad_lectura' (que registra por
-- contenido individual), esta tabla agrega el progreso a nivel
-- de módulo, alimentada desde el frontend cuando el empleado
-- marca contenidos como completados en la página del módulo.
--
-- Permite:
--   - Sincronizar progreso entre dispositivos (no solo localStorage)
--   - Disparar generación de certificado al completar (porcentaje = 100)
--   - Mostrar progreso real en dashboards
-- =============================================================

CREATE TABLE IF NOT EXISTS modulo_progreso (
    progreso_id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id               UUID         NOT NULL,
    modulo_id                UUID         NOT NULL,
    empresa_id               UUID,
    contenidos_completados   INT          NOT NULL DEFAULT 0,
    total_contenidos         INT          NOT NULL DEFAULT 0,
    porcentaje               INT          NOT NULL DEFAULT 0,
    completado               BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_inicio             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_completado         TIMESTAMPTZ,
    actualizado_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_modulo_progreso_usuario_modulo UNIQUE (usuario_id, modulo_id),
    CONSTRAINT ck_porcentaje_rango CHECK (porcentaje >= 0 AND porcentaje <= 100)
);

CREATE INDEX IF NOT EXISTS idx_modulo_progreso_usuario   ON modulo_progreso (usuario_id);
CREATE INDEX IF NOT EXISTS idx_modulo_progreso_modulo    ON modulo_progreso (modulo_id);
CREATE INDEX IF NOT EXISTS idx_modulo_progreso_empresa   ON modulo_progreso (empresa_id);
CREATE INDEX IF NOT EXISTS idx_modulo_progreso_completado ON modulo_progreso (completado) WHERE completado = TRUE;
