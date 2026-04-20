-- Tabla para tokens de recuperación de contraseña
-- Ejecutar en la base de datos de producción (Neon.tech)

CREATE TABLE IF NOT EXISTS password_reset_token (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token           VARCHAR(100) NOT NULL UNIQUE,
    usuario_id      UUID NOT NULL REFERENCES usuario(usuario_id) ON DELETE CASCADE,
    expira_en       TIMESTAMPTZ NOT NULL,
    usado           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_token ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_usuario ON password_reset_token(usuario_id);
