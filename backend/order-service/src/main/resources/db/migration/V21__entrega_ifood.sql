-- Origem "iFood" pra uma entrega, mesmo padrão de origem_whatsapp/origem_pdv.
-- ifood_order_id correlaciona com o pedido lá no iFood, necessário pra
-- avançar o status (confirmar/pronto/saiu/entregue) de volta pra eles.
ALTER TABLE entregas ADD COLUMN origem_ifood BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE entregas ADD COLUMN ifood_order_id VARCHAR(100);

CREATE INDEX idx_entregas_ifood_order_id ON entregas (restaurante_id, ifood_order_id) WHERE ifood_order_id IS NOT NULL;
