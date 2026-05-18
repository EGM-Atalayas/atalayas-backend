-- =============================================================
-- Migración: añadir ubicación a eventos de comunidad
-- =============================================================
-- Añade soporte para ubicación física del evento con mapa.
--   - lugar    : dirección o nombre del lugar (texto libre)
--   - latitud  : coordenada decimal (-90..90)
--   - longitud : coordenada decimal (-180..180)
-- Los tres son opcionales: el frontend puede pedirlos pero el
-- backend tolera eventos sin ubicación.
-- =============================================================

ALTER TABLE evento_comunidad
    ADD COLUMN IF NOT EXISTS lugar    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS latitud  NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS longitud NUMERIC(10, 7);

-- Constraints para evitar coordenadas absurdas
ALTER TABLE evento_comunidad
    DROP CONSTRAINT IF EXISTS ck_evento_latitud_rango,
    ADD  CONSTRAINT       ck_evento_latitud_rango
        CHECK (latitud IS NULL OR (latitud BETWEEN -90 AND 90));

ALTER TABLE evento_comunidad
    DROP CONSTRAINT IF EXISTS ck_evento_longitud_rango,
    ADD  CONSTRAINT       ck_evento_longitud_rango
        CHECK (longitud IS NULL OR (longitud BETWEEN -180 AND 180));
