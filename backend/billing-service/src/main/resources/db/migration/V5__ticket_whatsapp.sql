-- Permite tickets de suporte originados por WhatsApp (sem restaurante
-- vinculado — o contato é identificado só pelo telefone) além dos criados
-- pelo painel admin de um restaurante.
ALTER TABLE tickets_suporte ALTER COLUMN restaurante_id DROP NOT NULL;
ALTER TABLE tickets_suporte ADD COLUMN whatsapp_telefone VARCHAR(30);
ALTER TABLE tickets_suporte ADD COLUMN whatsapp_nome VARCHAR(150);
ALTER TABLE tickets_suporte ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'PAINEL';

CREATE INDEX idx_tickets_suporte_whatsapp_telefone ON tickets_suporte (whatsapp_telefone);
