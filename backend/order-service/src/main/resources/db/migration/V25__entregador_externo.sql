-- Restaurante sem entregador próprio (usa 99/Uber Entrega etc.) — ver RestauranteConfig.
ALTER TABLE restaurante_configs ADD COLUMN entregador_externo BOOLEAN NOT NULL DEFAULT false;
