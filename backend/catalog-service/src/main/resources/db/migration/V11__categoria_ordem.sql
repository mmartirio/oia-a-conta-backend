-- Ordem de exibição das categorias, ajustável pelo admin — antes a ordem
-- era sempre alfabética. Backfill preserva a ordem alfabética atual como
-- ponto de partida, pra não embaralhar o cardápio de quem já tem categorias
-- cadastradas.
ALTER TABLE categorias ADD COLUMN ordem INTEGER NOT NULL DEFAULT 0;

UPDATE categorias c
SET ordem = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY restaurante_id ORDER BY nome ASC) AS rn
    FROM categorias
) sub
WHERE c.id = sub.id;
