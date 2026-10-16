-- Metas mensais do restaurante (aba Metas do Financeiro): faturamento,
-- lucro e teto de despesas. Uma linha por restaurante por mês (YYYY-MM).
CREATE TABLE metas_financeiras (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    mes VARCHAR(7) NOT NULL,
    meta_faturamento NUMERIC(12,2),
    meta_lucro NUMERIC(12,2),
    limite_despesas NUMERIC(12,2),
    atualizado_em TIMESTAMP,
    CONSTRAINT uk_metas_restaurante_mes UNIQUE (restaurante_id, mes)
);
