-- Índice em coluna usada para filtrar por tenant (multi-tenant: restaurante_id).
CREATE INDEX idx_usuarios_restaurante_id ON usuarios (restaurante_id);
