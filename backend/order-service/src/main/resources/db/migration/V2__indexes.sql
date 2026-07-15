-- Índices em restaurante_id (multi-tenant), status, e compostos
-- (restaurante_id, status, created_at) nas tabelas mais consultadas por
-- esses filtros combinados (pedidos, entregas, comandas), além de índices
-- nas FKs de itens_pedido/itens_entrega usadas pelos @EntityGraph novos.

CREATE INDEX idx_comandas_restaurante_id ON comandas (restaurante_id);
CREATE INDEX idx_comandas_status ON comandas (status);
CREATE INDEX idx_comandas_restaurante_status_created_at ON comandas (restaurante_id, status, created_at);

CREATE INDEX idx_pedidos_restaurante_id ON pedidos (restaurante_id);
CREATE INDEX idx_pedidos_status ON pedidos (status);
CREATE INDEX idx_pedidos_restaurante_status_created_at ON pedidos (restaurante_id, status, created_at);
CREATE INDEX idx_pedidos_comanda_id ON pedidos (comanda_id);

CREATE INDEX idx_entregas_restaurante_id ON entregas (restaurante_id);
CREATE INDEX idx_entregas_status ON entregas (status);
CREATE INDEX idx_entregas_restaurante_status_created_at ON entregas (restaurante_id, status, created_at);

CREATE INDEX idx_itens_pedido_pedido_id ON itens_pedido (pedido_id);
CREATE INDEX idx_itens_entrega_entrega_id ON itens_entrega (entrega_id);
