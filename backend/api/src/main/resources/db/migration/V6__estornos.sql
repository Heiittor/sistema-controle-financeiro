-- Liga o estorno à transação original. UNIQUE: uma transação só pode ser estornada uma vez.
ALTER TABLE transacoes ADD COLUMN estorno_de_id BIGINT;
ALTER TABLE transacoes ADD CONSTRAINT fk_transacoes_estorno_de
    FOREIGN KEY (estorno_de_id) REFERENCES transacoes (id);
ALTER TABLE transacoes ADD CONSTRAINT uk_transacoes_estorno_de UNIQUE (estorno_de_id);

-- Amplia os tipos permitidos.
ALTER TABLE transacoes DROP CONSTRAINT ck_transacoes_tipo;
ALTER TABLE transacoes ADD CONSTRAINT ck_transacoes_tipo
    CHECK (tipo IN ('RECEITA', 'DESPESA', 'TRANSFERENCIA_SAIDA', 'TRANSFERENCIA_ENTRADA',
                    'ESTORNO_RECEITA', 'ESTORNO_DESPESA'));

-- Coerência entre tipo, categoria, transferencia_id e estorno_de_id.
ALTER TABLE transacoes DROP CONSTRAINT ck_transacoes_coerencia;
ALTER TABLE transacoes ADD CONSTRAINT ck_transacoes_coerencia
    CHECK (
        (tipo IN ('RECEITA', 'DESPESA')
            AND categoria_id IS NOT NULL AND transferencia_id IS NULL AND estorno_de_id IS NULL)
            OR
        (tipo IN ('TRANSFERENCIA_SAIDA', 'TRANSFERENCIA_ENTRADA')
            AND categoria_id IS NULL AND transferencia_id IS NOT NULL AND estorno_de_id IS NULL)
            OR
        (tipo IN ('ESTORNO_RECEITA', 'ESTORNO_DESPESA')
            AND categoria_id IS NULL AND transferencia_id IS NULL AND estorno_de_id IS NOT NULL)
        );