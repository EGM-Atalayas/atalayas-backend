-- =============================================================
-- Limpieza de datos de prueba en progreso (modulo_progreso)
-- =============================================================
-- Uso: psql -U <user> -d <db> -f cleanup_test_progreso.sql
-- O desde tu IDE de BD, pega cada bloque por separado.
-- =============================================================

-- 1) REVISAR: datos actuales en modulo_progreso
--    Descomenta y ejecuta para ver qué hay antes de borrar:
/*
SELECT mp.progreso_id,
       u.email          AS usuario_email,
       u.nombre         AS usuario_nombre,
       m.nombre         AS modulo_nombre,
       mp.porcentaje,
       mp.completado,
       mp.actualizado_en
FROM modulo_progreso mp
LEFT JOIN usuario u  ON u.usuario_id = mp.usuario_id
LEFT JOIN modulo  m  ON m.modulo_id  = mp.modulo_id
ORDER BY mp.actualizado_en DESC;
*/

-- 2) REVISAR: datos actuales en trazabilidad_lectura
/*
SELECT up.registro_id,
       u.email          AS usuario_email,
       c.titulo         AS contenido_titulo,
       up.completado,
       up.porcentaje_completado
FROM trazabilidad_lectura up
LEFT JOIN usuario   u ON u.usuario_id = up.usuario_id
LEFT JOIN contenido c ON c.contenido_id = up.contenido_id
ORDER BY up.actualizado_en DESC;
*/

-- 3) BORRAR progresos de prueba de modulo_progreso
--    Ajusta los filtros según lo que quieras eliminar.
--    Ejemplo: borrar progresos donde el módulo ya no existe (huérfanos):
DELETE FROM modulo_progreso mp
WHERE NOT EXISTS (SELECT 1 FROM modulo m WHERE m.modulo_id = mp.modulo_id);

--    Ejemplo: borrar progresos de usuarios concretos de prueba:
--    (descomenta y ajusta emails)
-- DELETE FROM modulo_progreso
-- WHERE usuario_id IN (SELECT usuario_id FROM usuario WHERE email IN ('test@example.com', 'prueba@atalayas.com'));

-- 4) BORRAR trazabilidad_lectura huérfana (contenido ya eliminado)
DELETE FROM trazabilidad_lectura up
WHERE NOT EXISTS (SELECT 1 FROM contenido c WHERE c.contenido_id = up.contenido_id);

-- 5) BORRAR progreso de modulo_progreso para usuarios que ya no existen
DELETE FROM modulo_progreso mp
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.usuario_id = mp.usuario_id);

-- 6) BORRAR trazabilidad_lectura para usuarios que ya no existen
DELETE FROM trazabilidad_lectura up
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.usuario_id = up.usuario_id);

-- 7) BORRAR progresos de prueba de empresas de prueba
--    (si hay empresas dadas de alta solo para pruebas)
-- DELETE FROM modulo_progreso mp
-- WHERE mp.empresa_id IN (SELECT empresa_id FROM empresa WHERE cif LIKE 'TEST%' OR nombre_empresa ILIKE '%test%');

-- DELETE FROM trazabilidad_lectura up
-- WHERE up.empresa_id IN (SELECT empresa_id FROM empresa WHERE cif LIKE 'TEST%' OR nombre_empresa ILIKE '%test%');
