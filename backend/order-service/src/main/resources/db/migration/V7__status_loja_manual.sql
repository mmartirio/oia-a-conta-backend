-- Toggle manual de abrir/fechar a loja, independente de pausas programadas.
ALTER TABLE restaurante_configs
    ADD COLUMN fechado_manualmente BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_fechamento_manual VARCHAR(255);
