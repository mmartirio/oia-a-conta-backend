CREATE TABLE ifood_mapeamentos (
    id              BIGSERIAL PRIMARY KEY,
    restaurante_id  BIGINT NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    local_id        BIGINT NOT NULL,
    ifood_id        VARCHAR(100) NOT NULL,
    atualizado_em   TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT uk_ifood_mapeamento_local UNIQUE (restaurante_id, tipo, local_id)
);

CREATE INDEX idx_ifood_mapeamento_ifood_id ON ifood_mapeamentos (restaurante_id, tipo, ifood_id);
