-- Suporte a cliente identificado + desconto (cupom ou promoção) aplicado à
-- comanda. cliente_id é referência cross-service ao catalog-service (sem FK,
-- mesmo padrão de restaurante_id). desconto/valor_total viram snapshot
-- persistido porque desconto não é derivável dos itens (vem de uma regra
-- externa) e valor_total evita recalcular a mesma soma em 3 lugares.
ALTER TABLE comandas ADD COLUMN cliente_id BIGINT;
ALTER TABLE comandas ADD COLUMN desconto_tipo VARCHAR(10) CHECK (desconto_tipo IN ('CUPOM','PROMOCAO'));
ALTER TABLE comandas ADD COLUMN desconto_origem_id BIGINT;
ALTER TABLE comandas ADD COLUMN desconto_origem_descricao VARCHAR(150);
ALTER TABLE comandas ADD COLUMN desconto NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE comandas ADD COLUMN valor_total NUMERIC(10,2);

CREATE INDEX idx_comandas_cliente_id ON comandas (cliente_id);
