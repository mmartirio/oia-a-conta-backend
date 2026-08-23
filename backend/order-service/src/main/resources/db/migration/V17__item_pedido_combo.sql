-- Combos são expandidos em linhas de produto reais (produto_id continua
-- sempre preenchido, estoque/cozinha enxergam produtos normais) — estas
-- colunas são só pra agrupar visualmente no recibo/KDS de onde vieram.
ALTER TABLE itens_pedido ADD COLUMN combo_id BIGINT;
ALTER TABLE itens_pedido ADD COLUMN combo_nome VARCHAR(150);
