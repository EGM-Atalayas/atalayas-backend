-- Tabla: servicio
-- Servicios que EGM Atalayas ofrece directamente a los trabajadores del parque.
-- Son siempre globales (no pertenecen a ninguna empresa concreta).

CREATE TABLE IF NOT EXISTS servicio (
    servicio_id    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    titulo         VARCHAR(255) NOT NULL,
    descripcion    TEXT,
    categoria      VARCHAR(50)  NOT NULL,  -- MOVILIDAD | INSTALACIONES | INICIATIVAS | COMUNES
    icono_url      VARCHAR(500),
    url_info       VARCHAR(500),
    telefono       VARCHAR(30),
    como_acceder   TEXT,
    creado_por     UUID,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Índice para filtrar por categoría rápidamente
CREATE INDEX IF NOT EXISTS idx_servicio_categoria ON servicio (categoria);
CREATE INDEX IF NOT EXISTS idx_servicio_activo    ON servicio (activo);
