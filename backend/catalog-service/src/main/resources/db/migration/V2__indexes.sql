-- Índices em colunas usadas para filtrar por tenant (multi-tenant: restaurante_id)
-- e no hot path de listarPorCategoria (categoria_id).

CREATE INDEX idx_categorias_restaurante_id ON categorias (restaurante_id);
CREATE INDEX idx_produtos_restaurante_id ON produtos (restaurante_id);
CREATE INDEX idx_produtos_categoria_id ON produtos (categoria_id);
