-- Tempo de preparo próprio do produto (minutos) — se nulo, o painel de
-- Delivery usa o tempo padrão do restaurante (ver tempo_preparo_padrao_minutos
-- em restaurante_configs, order-service). tempo_preparo_quantidade_limite +
-- tempo_preparo_minutos_acima_limite formam uma regra opcional: a partir
-- dessa quantidade no pedido, usa o tempo "acima do limite" em vez do normal
-- (ex: até 4 unidades = 15 min, 5 ou mais = 25 min).
ALTER TABLE produtos ADD COLUMN tempo_preparo_minutos INTEGER;
ALTER TABLE produtos ADD COLUMN tempo_preparo_quantidade_limite INTEGER;
ALTER TABLE produtos ADD COLUMN tempo_preparo_minutos_acima_limite INTEGER;
