-- Combo no carrinho do cardápio público: produto_id fica nulo (não é um
-- produto único) e combo_id/combo_nome identificam o combo. produto_nome
-- continua preenchido com o nome de exibição (do produto OU do combo), pra
-- não precisar de tratamento especial no texto de confirmação do WhatsApp.
ALTER TABLE itens_carrinho ALTER COLUMN produto_id DROP NOT NULL;
ALTER TABLE itens_carrinho ADD COLUMN combo_id BIGINT;
ALTER TABLE itens_carrinho ADD COLUMN combo_nome VARCHAR(150);
