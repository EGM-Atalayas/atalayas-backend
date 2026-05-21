-- Añade la columna fecha_baja a la tabla usuario
-- Se rellena automáticamente cuando un usuario es desactivado (activo = false)
-- Ejecutar en la base de datos de producción

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS fecha_baja TIMESTAMP WITH TIME ZONE;

-- Opcional: para usuarios ya inactivos sin fecha_baja, usar actualizado_en como aproximación
UPDATE usuario
SET fecha_baja = actualizado_en
WHERE activo = false AND fecha_baja IS NULL;
