-- Migración completa: añade todas las columnas opcionales a la tabla usuario
-- Seguro ejecutar múltiples veces (IF NOT EXISTS)

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS banner_url      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bio             VARCHAR(300),
    ADD COLUMN IF NOT EXISTS telefono        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS disponibilidad  VARCHAR(20) DEFAULT 'DISPONIBLE',
    ADD COLUMN IF NOT EXISTS puesto_trabajo  VARCHAR(150),
    ADD COLUMN IF NOT EXISTS modo_oscuro     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS notif_nuevo_modulo       BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_modulo_completado  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_comunicado         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_pendiente          BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS terminos_aceptados       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS intentos_fallidos        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fecha_baja      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ultimo_login    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW();


