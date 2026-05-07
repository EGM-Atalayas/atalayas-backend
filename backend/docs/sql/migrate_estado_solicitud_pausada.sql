-- ============================================================
-- Migración: añadir estado PAUSADA, eliminar RECHAZADA
-- Aplicar manualmente en la BD de Supabase (ddl-auto=none)
-- Fecha: Abril 2026
-- ============================================================

-- 1. Eliminar el CHECK constraint anterior que incluía 'RECHAZADA'
--    (el nombre puede variar; consultar con \d empresa si falla)
ALTER TABLE empresa DROP CONSTRAINT IF EXISTS empresa_estado_solicitud_check;

-- 2. Crear el CHECK constraint actualizado con los tres estados válidos
ALTER TABLE empresa
    ADD CONSTRAINT empresa_estado_solicitud_check
    CHECK (estado_solicitud IN ('PENDIENTE', 'APROBADA', 'PAUSADA'));

-- NOTA: Si existieran filas con estado_solicitud = 'RECHAZADA' en producción,
-- deben ser eliminadas antes de ejecutar el paso 2:
--   DELETE FROM usuario WHERE empresa_id IN (SELECT empresa_id FROM empresa WHERE estado_solicitud = 'RECHAZADA');
--   DELETE FROM empresa WHERE estado_solicitud = 'RECHAZADA';

