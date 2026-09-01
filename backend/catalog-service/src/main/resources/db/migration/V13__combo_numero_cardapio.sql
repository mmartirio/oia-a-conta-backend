-- Numeração do cardápio pro chatbot do WhatsApp também precisa cobrir
-- combos, não só produtos (ver Produto.numeroCardapio / V12) — sem isso,
-- um cliente que responde com o número de um combo no chat é ignorado
-- silenciosamente.
ALTER TABLE combos ADD COLUMN numero_cardapio INTEGER;
