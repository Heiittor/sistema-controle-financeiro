CREATE TABLE orcamentos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    categoria_id BIGINT NOT NULL REFERENCES categorias(id),
    mes DATE NOT NULL CHECK (extract(day from mes) = 1),
    limite NUMERIC(19,2) NOT NULL CHECK (limite > 0),
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_orcamentos_usuario_categoria_mes UNIQUE (usuario_id, categoria_id, mes)
);
CREATE INDEX idx_orcamentos_usuario_mes ON orcamentos(usuario_id, mes);
