-- Ampliar CHECK constraint de incidencia para soportar EN_CURSO y RESUELTA
-- Ejecutar una sola vez en producción (Supabase)

ALTER TABLE public.incidencia DROP CONSTRAINT IF EXISTS incidencia_estado_check;

ALTER TABLE public.incidencia
    ADD CONSTRAINT incidencia_estado_check
    CHECK (estado IN ('ABIERTA', 'EN_CURSO', 'RESUELTA', 'CERRADA'));

-- Verificar
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'public.incidencia'::regclass
  AND conname = 'incidencia_estado_check';

