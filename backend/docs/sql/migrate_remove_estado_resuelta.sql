-- Eliminar estado RESUELTA: convertir filas existentes a CERRADA y actualizar constraint
-- Ejecutar una sola vez en producción (Supabase)

-- 1. Convertir incidencias existentes con estado RESUELTA → CERRADA
UPDATE public.incidencia
SET estado = 'CERRADA'
WHERE estado = 'RESUELTA';

-- 2. Reemplazar el CHECK constraint sin RESUELTA
ALTER TABLE public.incidencia DROP CONSTRAINT IF EXISTS incidencia_estado_check;

ALTER TABLE public.incidencia
    ADD CONSTRAINT incidencia_estado_check
    CHECK (estado IN ('ABIERTA', 'EN_CURSO', 'CERRADA'));

-- Verificar resultado
SELECT estado, COUNT(*) FROM public.incidencia GROUP BY estado;
