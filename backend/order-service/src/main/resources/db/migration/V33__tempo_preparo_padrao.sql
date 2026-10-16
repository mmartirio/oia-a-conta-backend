-- Tempo de preparo padrão do restaurante (minutos), usado no cronômetro do
-- painel de Delivery quando o produto do pedido não tem tempo próprio
-- configurado (ver tempo_preparo_minutos em produtos, catalog-service).
ALTER TABLE restaurante_configs ADD COLUMN tempo_preparo_padrao_minutos INTEGER NOT NULL DEFAULT 15;
