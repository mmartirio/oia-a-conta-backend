-- Preferência de som de alerta (campainha) tocado quando um pedido de
-- cliente chega aguardando aceite/rejeição da cozinha. Três opções fixas no
-- frontend (CLASSICA, DUPLA, URGENTE) — guardamos só a chave escolhida.
ALTER TABLE restaurante_configs ADD COLUMN alerta_pedido_som VARCHAR(20) NOT NULL DEFAULT 'CLASSICA';
