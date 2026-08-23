-- Toggle: quando ligado, mensagem nova do WhatsApp toca a notificação falada
-- em vez da notificação padrão (ver AdminConfiguracoes / NotificationContext).
ALTER TABLE restaurante_configs ADD COLUMN notificacao_whatsapp_falada BOOLEAN NOT NULL DEFAULT false;
