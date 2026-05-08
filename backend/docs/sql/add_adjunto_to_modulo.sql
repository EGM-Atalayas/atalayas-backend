-- Añade las columnas adjunto_url y adjunto_nombre a la tabla modulo
-- Ejecutar en la base de datos de producción

ALTER TABLE modulo
    ADD COLUMN IF NOT EXISTS adjunto_url     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS adjunto_nombre  VARCHAR(255);
