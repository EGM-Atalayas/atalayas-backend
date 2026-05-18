-- =============================================================
-- Migración: añadir imagen de portada a eventos de comunidad
-- =============================================================
-- Columna opcional para almacenar la URL pública de la imagen de
-- portada del evento. El archivo en sí se sube via POST /upload/imagen
-- al bucket "modulos" de Supabase Storage; aquí solo guardamos la URL.
-- =============================================================

ALTER TABLE evento_comunidad
    ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(500);
