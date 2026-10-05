CREATE TABLE categorias (
                            id         BIGSERIAL PRIMARY KEY,
                            nome       VARCHAR(100) NOT NULL,
                            tipo       VARCHAR(10)  NOT NULL,
                            usuario_id BIGINT       NOT NULL,
                            criado_em  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                            CONSTRAINT fk_categorias_usuario
                                FOREIGN KEY (usuario_id) REFERENCES usuarios (id),

                            CONSTRAINT ck_categorias_tipo
                                CHECK (tipo IN ('RECEITA', 'DESPESA')),

                            CONSTRAINT uk_categorias_usuario_nome_tipo
                                UNIQUE (usuario_id, nome, tipo)
);