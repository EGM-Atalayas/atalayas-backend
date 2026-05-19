-- Migración: actualizar CHECK constraint de tipo_modulo para aceptar valores del enum Java
-- Fecha: 2026-05-15
-- Contexto: el backend usa EnumType.STRING con valores largos (ej. RECOMPENSAS_VENTAJAS)
--           pero el constraint antiguo sólo aceptaba los valores cortos originales.
--
-- INSTRUCCIONES:
--   1. Hacer backup de la tabla modulo antes de ejecutar.
--   2. Ejecutar en psql o pgAdmin contra la BD de destino (staging/producción).
--   3. Verificar con: \d modulo  (en psql) o consultando information_schema.check_constraints

BEGIN;

-- ── DIAGNÓSTICO (opcional — ejecutar antes para revisar el estado actual) ──────
-- SELECT tipo_modulo, COUNT(*) FROM modulo GROUP BY tipo_modulo ORDER BY tipo_modulo;
-- SELECT conname, consrc FROM pg_constraint WHERE conname = 'modulo_tipo_modulo_check';

-- ── 1. Eliminar el CHECK constraint PRIMERO (antes de cualquier UPDATE) ────────
--    El constraint antiguo rechaza los valores nuevos incluso en los propios UPDATEs
ALTER TABLE public.modulo DROP CONSTRAINT IF EXISTS modulo_tipo_modulo_check;

-- ── 2. Migrar filas con valores legacy ────────────────────────────────────────
--    GENERAL       → FORMACION_BASICA
--    ESPECIALIZADO → FORMACION_ESPECIFICA
UPDATE public.modulo SET tipo_modulo = 'FORMACION_BASICA'    WHERE tipo_modulo = 'GENERAL';
UPDATE public.modulo SET tipo_modulo = 'FORMACION_ESPECIFICA' WHERE tipo_modulo = 'ESPECIALIZADO';


-- ── 3. Recrear el CHECK con TODOS los valores actuales del enum ModuleType ─────
--    Incluye legacy por compatibilidad con datos no migrados (ESPECIALIZADO_IA no es legacy)
ALTER TABLE public.modulo
    ADD CONSTRAINT modulo_tipo_modulo_check CHECK (
        tipo_modulo::text = ANY (ARRAY[
            -- Activos
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
            'ONBOARDING',
            'ESPECIALIZADO_IA',
            -- Legacy (mantenidos por compatibilidad histórica)
            'GENERAL',
            'ESPECIALIZADO'
        ]::text[])
    );

-- ── 4. Verificación post-migración ────────────────────────────────────────────
-- Debe mostrar 1 fila con el nuevo constraint:
-- SELECT conname FROM pg_constraint WHERE conname = 'modulo_tipo_modulo_check';

COMMIT;


