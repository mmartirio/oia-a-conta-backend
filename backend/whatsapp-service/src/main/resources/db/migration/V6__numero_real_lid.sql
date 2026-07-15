-- Contatos "@lid" (recurso de privacidade do WhatsApp) não expõem o número
-- de telefone real na API — o chatbot pergunta o número ao cliente no
-- primeiro contato e guarda aqui, separado da coluna "telefone" (que continua
-- sendo o JID/lid usado para roteamento de envio, o único endereço que
-- funciona para esses contatos).
ALTER TABLE sessoes_whatsapp ADD COLUMN numero_real VARCHAR(20);
