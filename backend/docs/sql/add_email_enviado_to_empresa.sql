-- ============================================================
-- Migración: añadir columna email_enviado a la tabla empresa
-- Prerequisito: endpoint POST /api/v1/empresas/{id}/reenviar-email
-- Aplicar manualmente en la BD de Neon.tech (ddl-auto=none)
-- Fecha: Mayo 2026
-- ============================================================

-- 1. Añadir la columna con valor por defecto FALSE para nuevas filas
ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS email_enviado BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Backfill: las empresas ya APROBADAS se consideran con email enviado
--    (fueron aprobadas antes de esta columna — asumimos email OK)
UPDATE empresa
    SET email_enviado = TRUE
    WHERE estado_solicitud = 'APROBADA';

-- NOTA: Las empresas en estado PENDIENTE quedan con email_enviado = FALSE,
-- que es correcto: su email de aprobación aún no se ha enviado.

