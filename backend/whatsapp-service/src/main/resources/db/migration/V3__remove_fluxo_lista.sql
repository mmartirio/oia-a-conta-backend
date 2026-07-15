-- Remove colunas usadas apenas pelo fluxo legado de navegação por lista de
-- texto (categorias/produtos numerados no chat), removido do ChatbotService.
-- O pedido hoje é sempre feito pelo cardápio digital (web); a sessão do
-- WhatsApp só coleta endereço/pagamento/observação a partir daí.
ALTER TABLE sessoes_whatsapp DROP COLUMN IF EXISTS categoria_selecionada_id;
ALTER TABLE sessoes_whatsapp DROP COLUMN IF EXISTS produto_selecionado_id;
