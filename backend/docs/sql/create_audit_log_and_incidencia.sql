-- ──────────────────────────────────────────────────────────────────────────────
-- DDL: creación de las tablas audit_log e incidencia
--
-- audit_log   → alimenta actividadReciente del dashboard del superadmin.
-- incidencia  → gestión de incidencias de plataforma; empresa_id es nullable
--               (NULL = incidencia global, valor = asociada a empresa concreta).
--
-- ⚠️  Ejecutar una sola vez sobre la BD de producción / desarrollo.
-- ⚠️  La tabla 'empresa' debe existir antes de ejecutar este script.
-- ──────────────────────────────────────────────────────────────────────────────

-- ── audit_log ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGSERIAL    PRIMARY KEY,
    texto      VARCHAR(500) NOT NULL,
    tipo       VARCHAR(20)  NOT NULL,   -- 'info' | 'success' | 'warning' | 'error'
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_creado_en
    ON audit_log (creado_en DESC);

-- ── incidencia ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS incidencia (
    id          BIGSERIAL    PRIMARY KEY,
    titulo      VARCHAR(300) NOT NULL,
    descripcion TEXT,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA',   -- ABIERTA | CERRADA
    prioridad   VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',    -- NORMAL  | CRITICA
    empresa_id  UUID         REFERENCES empresa(empresa_id) ON DELETE SET NULL,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incidencia_estado
    ON incidencia (estado);

CREATE INDEX IF NOT EXISTS idx_incidencia_empresa_id
    ON incidencia (empresa_id);

CREATE INDEX IF NOT EXISTS idx_incidencia_creado_en
    ON incidencia (creado_en DESC);

