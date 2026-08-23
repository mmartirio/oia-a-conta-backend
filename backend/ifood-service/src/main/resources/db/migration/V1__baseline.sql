CREATE TABLE ifood_merchants (
    id                         BIGSERIAL PRIMARY KEY,
    restaurante_id             BIGINT NOT NULL UNIQUE,
    merchant_id                VARCHAR(100) NOT NULL,
    merchant_nome              VARCHAR(200),
    access_token               TEXT,
    refresh_token              TEXT,
    expira_em                  TIMESTAMP(6) WITHOUT TIME ZONE,
    ativo                      BOOLEAN NOT NULL DEFAULT TRUE,
    conectado_em               TIMESTAMP(6) WITHOUT TIME ZONE,
    ultimo_status_enviado      BOOLEAN,
    catalogo_sincronizado_em   TIMESTAMP(6) WITHOUT TIME ZONE
);
