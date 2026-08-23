-- Número do produto na imagem do cardápio numerado (chatbot do WhatsApp) —
-- opcional, definido pelo admin por produto, sem relação com a ordem de
-- exibição do cardápio público (essa é por categoria.ordem).
ALTER TABLE produtos ADD COLUMN numero_cardapio INTEGER;
