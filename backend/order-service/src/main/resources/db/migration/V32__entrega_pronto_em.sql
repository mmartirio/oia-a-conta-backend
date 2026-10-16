-- Momento em que a entrega virou PRONTO_PARA_ENTREGA — usado pra calcular o
-- cronômetro de "tempo de entrega" no painel (reinicia do zero a partir
-- daqui, diferente do cronômetro de "tempo de preparo" que usa created_at).
ALTER TABLE entregas ADD COLUMN pronto_em TIMESTAMP;
