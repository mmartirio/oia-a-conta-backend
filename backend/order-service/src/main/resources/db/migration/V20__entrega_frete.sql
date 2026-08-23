-- Coordenadas do endereço de entrega (geocodificado no frontend) e o frete
-- calculado a partir da distância até o restaurante. Tudo nullable: o
-- geocoding pode falhar, e nesse caso o pedido segue sem frete calculado.
ALTER TABLE entregas ADD COLUMN endereco_latitude DOUBLE PRECISION;
ALTER TABLE entregas ADD COLUMN endereco_longitude DOUBLE PRECISION;
ALTER TABLE entregas ADD COLUMN distancia_km NUMERIC(10,2);
ALTER TABLE entregas ADD COLUMN valor_frete NUMERIC(10,2);
