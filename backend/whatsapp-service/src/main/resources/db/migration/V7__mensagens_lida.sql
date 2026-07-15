-- Rastreamento de leitura das mensagens recebidas dos clientes, usado pra
-- contar "conversas novas" (com mensagem não lida) no badge do WhatsApp no
-- admin. Mensagens já existentes entram como lidas pra não gerar uma
-- contagem retroativa de não-lidas ao aplicar a migration.
ALTER TABLE mensagens_whatsapp ADD COLUMN lida BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_mensagens_whatsapp_nao_lidas
    ON mensagens_whatsapp (restaurante_id, telefone)
    WHERE direcao = 'RECEBIDA' AND lida = FALSE;
