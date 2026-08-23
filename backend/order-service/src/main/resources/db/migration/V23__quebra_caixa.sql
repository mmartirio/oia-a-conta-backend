-- Registra, no fechamento do caixa, quanto em dinheiro era esperado (abertura +
-- vendas em dinheiro da sessão) e a diferença em relação ao valor contado —
-- permite auditar sobra/falta de caixa sem misturar formas de pagamento que
-- não passam pela gaveta física (PIX, cartão).
ALTER TABLE sessoes_caixa ADD COLUMN valor_esperado_dinheiro NUMERIC(10,2);
ALTER TABLE sessoes_caixa ADD COLUMN diferenca_caixa NUMERIC(10,2);
