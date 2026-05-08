-- ──────────────────────────────────────────────────────────────────────────────
-- Backfill: corregir empresa_id NULL o incorrecto en trazabilidad_lectura
--
-- Problema: registros creados antes del fix donde empresa_id venía del body
-- del frontend y podía ser NULL o pertenecer a otra empresa.
--
-- Este script asigna el empresa_id correcto tomándolo del usuario dueño
-- del registro (trazabilidad_lectura.usuario_id → usuario.empresa_id).
--
-- ⚠️  Ejecutar en BD ANTES de desplegar la nueva versión del backend.
-- ⚠️  Hacer backup previo o ejecutar dentro de una transacción.
-- ──────────────────────────────────────────────────────────────────────────────

BEGIN;

-- 1. Diagnóstico previo — ver cuántos registros están afectados
SELECT
    COUNT(*) FILTER (WHERE t.empresa_id IS NULL)              AS empresa_id_null,
    COUNT(*) FILTER (WHERE t.empresa_id != u.empresa_id)      AS empresa_id_incorrecta,
    COUNT(*)                                                   AS total
FROM trazabilidad_lectura t
JOIN usuario u ON u.usuario_id = t.usuario_id;

-- 2. Corregir registros con empresa_id NULL
UPDATE trazabilidad_lectura t
SET empresa_id = u.empresa_id
FROM usuario u
WHERE t.usuario_id = u.usuario_id
  AND t.empresa_id IS NULL;

-- 3. Corregir registros con empresa_id incorrecto (no coincide con el del usuario)
UPDATE trazabilidad_lectura t
SET empresa_id = u.empresa_id
FROM usuario u
WHERE t.usuario_id = u.usuario_id
  AND t.empresa_id != u.empresa_id;

-- 4. Verificación post-update — debe devolver 0 en ambas columnas
SELECT
    COUNT(*) FILTER (WHERE t.empresa_id IS NULL)              AS empresa_id_null_restantes,
    COUNT(*) FILTER (WHERE t.empresa_id != u.empresa_id)      AS empresa_id_incorrecta_restantes
FROM trazabilidad_lectura t
JOIN usuario u ON u.usuario_id = t.usuario_id;

COMMIT;

