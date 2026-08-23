-- Corrige uma condição de corrida no chatbot: mensagens quase simultâneas do
-- mesmo número podiam cada uma achar "nenhuma sessão ainda" (find-then-create
-- sem lock) e criar linhas duplicadas pra (telefone, restaurante_id) —
-- quebrando qualquer query que espere uma sessão única por telefone (ex:
-- listagem de conversas). A sincronização por telefone em ChatbotService
-- evita que isso aconteça de novo; esse constraint é a proteção de última
-- linha no banco, caso rode mais de uma instância do serviço no futuro.
ALTER TABLE sessoes_whatsapp ADD CONSTRAINT uk_sessao_telefone_restaurante UNIQUE (telefone, restaurante_id);
