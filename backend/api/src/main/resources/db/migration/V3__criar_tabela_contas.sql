CREATE TABLE contas (
                        id         BIGSERIAL PRIMARY KEY,
                        nome       VARCHAR(100)  NOT NULL,
                        saldo      NUMERIC(19,2) NOT NULL DEFAULT 0,
                        version    BIGINT        NOT NULL DEFAULT 0,
                        usuario_id BIGINT        NOT NULL,
                        criado_em  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                        CONSTRAINT fk_contas_usuario
                            FOREIGN KEY (usuario_id) REFERENCES usuarios (id),

                        CONSTRAINT ck_contas_saldo_nao_negativo
                            CHECK (saldo >= 0),

                        CONSTRAINT uk_contas_usuario_nome
                            UNIQUE (usuario_id, nome)
);