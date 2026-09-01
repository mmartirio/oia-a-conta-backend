-- Combos deixam de ter itens fixos e passam a ter "grupos" (ex: "2 Pastéis",
-- "1 Refrigerante") com uma quantidade e uma lista de produtos elegíveis —
-- o cliente escolhe quais sabores quer dentro de cada grupo, sem alterar o
-- preço do combo (que é sempre fixo). Substitui combo_itens, que fica
-- órfã (mantida por compatibilidade histórica, sem uso a partir daqui).
CREATE TABLE combo_grupos (
    id          BIGSERIAL PRIMARY KEY,
    combo_id    BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    nome        VARCHAR(100) NOT NULL,
    quantidade  INTEGER NOT NULL CHECK (quantidade > 0),
    ordem       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE combo_grupo_produtos (
    id          BIGSERIAL PRIMARY KEY,
    grupo_id    BIGINT NOT NULL REFERENCES combo_grupos(id) ON DELETE CASCADE,
    produto_id  BIGINT NOT NULL REFERENCES produtos(id),
    UNIQUE(grupo_id, produto_id)
);

CREATE INDEX idx_combo_grupos_combo_id ON combo_grupos(combo_id);
CREATE INDEX idx_combo_grupo_produtos_grupo_id ON combo_grupo_produtos(grupo_id);
