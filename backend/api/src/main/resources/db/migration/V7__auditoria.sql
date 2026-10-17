CREATE TABLE auditoria (
                           id          BIGSERIAL PRIMARY KEY,
                           usuario_id  BIGINT       NOT NULL,
                           acao        VARCHAR(40)  NOT NULL,
                           entidade    VARCHAR(40)  NOT NULL,
                           entidade_id BIGINT       NOT NULL,
                           detalhes    VARCHAR(500),
                           criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- "O que este usuário fez e quando?"
CREATE INDEX idx_auditoria_usuario_data ON auditoria (usuario_id, criado_em);
-- "O que aconteceu com esta transação/conta?"
CREATE INDEX idx_auditoria_entidade ON auditoria (entidade, entidade_id);