-- Posição GPS atual do entregador enquanto uma entrega está "SAIU_PARA_ENTREGA",
-- usada pro mapa de rastreamento ao vivo no dashboard do admin. Guarda só a
-- posição mais recente (não o histórico da rota).
ALTER TABLE entregas ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE entregas ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE entregas ADD COLUMN localizacao_atualizada_em TIMESTAMP(6) WITHOUT TIME ZONE;
