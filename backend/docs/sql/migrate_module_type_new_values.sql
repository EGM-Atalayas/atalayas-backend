-- Migración: nuevos valores de tipo_modulo
-- Fecha: 2026-05-07

BEGIN;

-- 1. Migrar valores legacy antes de modificar el constraint
UPDATE modulo SET tipo_modulo = 'FORMACION_BASICA'   WHERE tipo_modulo = 'GENERAL';
UPDATE modulo SET tipo_modulo = 'FORMACION_ESPECIFICA' WHERE tipo_modulo = 'ESPECIALIZADO';

-- 2. Eliminar el constraint CHECK existente
ALTER TABLE public.modulo DROP CONSTRAINT modulo_tipo_modulo_check;

-- 3. Recrear el constraint con todos los valores válidos
ALTER TABLE public.modulo
    ADD CONSTRAINT modulo_tipo_modulo_check CHECK (
        tipo_modulo::text = ANY (ARRAY[
            'ESPECIALIZADO_IA',
            'IDENTIDAD_CORPORATIVA',
            'FORMACION_BASICA',
            'FORMACION_ESPECIFICA',
            'DESARROLLO_PROFESIONAL',
            'RECOMPENSAS_VENTAJAS',
            'COMUNIDAD',
            'CUMPLIMIENTO',
            'LIDERAZGO',
            'TECNICO',
            'SOFT_SKILLS',
            'ONBOARDING'
        ]::text[])
    );

COMMIT;


