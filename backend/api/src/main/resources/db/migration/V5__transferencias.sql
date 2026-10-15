-- Categoria deixa de ser obrigatória (transferências não têm categoria).
ALTER TABLE transacoes ALTER COLUMN categoria_id DROP NOT NULL;

-- Liga as duas pontas de uma mesma transferência.
ALTER TABLE transacoes ADD COLUMN transferencia_id UUID;

ALTER TABLE transacoes ALTER COLUMN tipo TYPE VARCHAR(25);
-- Amplia os tipos permitidos.
ALTER TABLE transacoes DROP CONSTRAINT ck_transacoes_tipo;
ALTER TABLE transacoes ADD CONSTRAINT ck_transacoes_tipo
    CHECK (tipo IN ('RECEITA', 'DESPESA', 'TRANSFERENCIA_SAIDA', 'TRANSFERENCIA_ENTRADA'));

-- Rede de segurança: coerência entre tipo, categoria e transferencia_id.
ALTER TABLE transacoes ADD CONSTRAINT ck_transacoes_coerencia
    CHECK (
        (tipo IN ('RECEITA', 'DESPESA')
            AND categoria_id IS NOT NULL AND transferencia_id IS NULL)
            OR
        (tipo IN ('TRANSFERENCIA_SAIDA', 'TRANSFERENCIA_ENTRADA')
            AND categoria_id IS NULL AND transferencia_id IS NOT NULL)
        );

CREATE INDEX idx_transacoes_transferencia ON transacoes (transferencia_id);