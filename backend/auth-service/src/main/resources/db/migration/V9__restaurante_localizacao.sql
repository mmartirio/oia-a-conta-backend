-- Ajuste manual da localização do restaurante no mapa (arrastar o marcador
-- no Dashboard) — sobrepõe a geocodificação automática do endereço quando
-- ela erra o ponto exato.
ALTER TABLE restaurantes ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE restaurantes ADD COLUMN longitude DOUBLE PRECISION;
