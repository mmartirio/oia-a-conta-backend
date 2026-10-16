-- Custo das entradas de estoque (compra de insumos) — alimenta a aba
-- Insumos do Financeiro (quanto foi gasto em compras no período e quanto
-- vale o estoque atual). Opcional: entradas antigas/sem custo ficam null.
ALTER TABLE movimentacoes_estoque ADD COLUMN custo_unitario NUMERIC(12,4);
ALTER TABLE movimentacoes_estoque ADD COLUMN valor_total NUMERIC(12,2);

-- Custo médio ponderado do saldo atual (atualizado a cada ENTRADA com custo).
ALTER TABLE estoque ADD COLUMN custo_medio NUMERIC(12,4);

CREATE INDEX idx_mov_estoque_rest_tipo_data ON movimentacoes_estoque (restaurante_id, tipo, criado_em);
