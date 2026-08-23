-- Combos no pedido público/WhatsApp são expandidos nos produtos reais que os
-- compõem (produto_id continua sempre preenchido) — estas colunas são só pra
-- agrupar visualmente no recibo/KDS de onde a linha veio, espelhando
-- itens_pedido.combo_id/combo_nome (V17).
ALTER TABLE itens_entrega ADD COLUMN combo_id BIGINT;
ALTER TABLE itens_entrega ADD COLUMN combo_nome VARCHAR(150);
