-- Índices em colunas usadas para filtrar por tenant e no hot path de
-- ChatbotService.processarMensagem (findByTelefoneAndRestauranteId, chamado
-- a cada mensagem recebida do webhook). mensagem_template.restaurante_id já
-- tem um índice implícito via a unique constraint (restaurante_id, chave).

CREATE INDEX idx_sessoes_whatsapp_restaurante_id ON sessoes_whatsapp (restaurante_id);
CREATE INDEX idx_sessoes_whatsapp_telefone_restaurante_id ON sessoes_whatsapp (telefone, restaurante_id);
CREATE INDEX idx_sessoes_whatsapp_entrega_id ON sessoes_whatsapp (entrega_id);
CREATE INDEX idx_itens_carrinho_sessao_id ON itens_carrinho (sessao_id);
