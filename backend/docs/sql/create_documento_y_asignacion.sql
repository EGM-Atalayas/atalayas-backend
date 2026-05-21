-- Tablas para gestión documental
-- Documentos subidos por admin y asignaciones a empleados

CREATE TABLE IF NOT EXISTS documento (
    documento_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID NOT NULL,
    titulo            VARCHAR(200) NOT NULL,
    descripcion       VARCHAR(500),
    tipo              VARCHAR(30) NOT NULL,
    archivo_url       VARCHAR(500) NOT NULL,
    archivo_nombre    VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(100),
    tamano_bytes      BIGINT,
    subido_por        UUID NOT NULL,
    requiere_firma    BOOLEAN NOT NULL DEFAULT FALSE,
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_subida      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    actualizado_en    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documento_empresa ON documento (empresa_id);
CREATE INDEX IF NOT EXISTS idx_documento_tipo    ON documento (tipo);
CREATE INDEX IF NOT EXISTS idx_documento_activo  ON documento (activo);

CREATE TABLE IF NOT EXISTS documento_asignacion (
    asignacion_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id      UUID NOT NULL REFERENCES documento(documento_id) ON DELETE CASCADE,
    usuario_id        UUID NOT NULL,
    fecha_asignacion  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    visto             BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_visto       TIMESTAMP WITH TIME ZONE,
    firmado           BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_firma       TIMESTAMP WITH TIME ZONE,
    firma_url         VARCHAR(500),
    UNIQUE (documento_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_asignacion_usuario   ON documento_asignacion (usuario_id);
CREATE INDEX IF NOT EXISTS idx_asignacion_documento ON documento_asignacion (documento_id);
