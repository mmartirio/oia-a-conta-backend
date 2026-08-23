-- Preço do frete por km: valorFrete = frete_taxa_base + frete_valor_por_km * distância.
-- Configurável pelo admin, mesmo padrão das taxas da maquininha.
ALTER TABLE restaurante_configs ADD COLUMN frete_taxa_base NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE restaurante_configs ADD COLUMN frete_valor_por_km NUMERIC(10,2) NOT NULL DEFAULT 0;
