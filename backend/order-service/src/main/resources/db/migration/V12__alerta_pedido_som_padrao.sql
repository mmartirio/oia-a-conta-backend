-- V11 usou 'CLASSICA' como valor padrão (nomes de campainhas sintetizadas).
-- O alerta passou a usar 3 arquivos de áudio reais, renomeados para
-- SOM_1/SOM_2/SOM_3 — atualiza o default da coluna e os valores já gravados.
ALTER TABLE restaurante_configs ALTER COLUMN alerta_pedido_som SET DEFAULT 'SOM_1';
UPDATE restaurante_configs SET alerta_pedido_som = 'SOM_1' WHERE alerta_pedido_som = 'CLASSICA';
