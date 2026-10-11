CREATE TABLE transacoes (
                            id           BIGSERIAL PRIMARY KEY,
                            descricao    VARCHAR(255)  NOT NULL,
                            valor        NUMERIC(19,2) NOT NULL,
                            data         DATE          NOT NULL,
                            tipo         VARCHAR(10)   NOT NULL,
                            conta_id     BIGINT        NOT NULL,
                            categoria_id BIGINT        NOT NULL,
                            criado_em    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                            CONSTRAINT fk_transacoes_conta
                                FOREIGN KEY (conta_id) REFERENCES contas (id),

                            CONSTRAINT fk_transacoes_categoria
                                FOREIGN KEY (categoria_id) REFERENCES categorias (id),

                            CONSTRAINT ck_transacoes_valor_positivo
                                CHECK (valor > 0),

                            CONSTRAINT ck_transacoes_tipo
                                CHECK (tipo IN ('RECEITA', 'DESPESA'))
);

CREATE INDEX idx_transacoes_conta_data ON transacoes (conta_id, data);
CREATE INDEX idx_transacoes_categoria ON transacoes (categoria_id);