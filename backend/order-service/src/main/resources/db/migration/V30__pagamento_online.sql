-- Pagamento online no cardápio digital (Mercado Pago): cada restaurante
-- conecta a PRÓPRIA conta do MP via OAuth, e o dinheiro do pedido cai
-- direto nela — a plataforma não intermedeia o valor. Tokens guardados
-- cifrados (AES-GCM, chave em MP_TOKEN_KEY — ver TokenCifrador).
CREATE TABLE mercadopago_contas (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL UNIQUE,
    mp_user_id VARCHAR(50),
    access_token_cifrado TEXT,
    refresh_token_cifrado TEXT,
    public_key VARCHAR(100),
    expira_em TIMESTAMP,
    conectado_em TIMESTAMP,
    -- "state" do OAuth em andamento (anti-CSRF) — limpo ao concluir.
    oauth_state VARCHAR(100),
    oauth_state_expira_em TIMESTAMP
);

-- Pedido pago online fica em AGUARDANDO_PAGAMENTO (invisível pra cozinha e
-- sem notificar o restaurante) até o MP aprovar o pagamento; aí entra no
-- fluxo normal em AGUARDANDO.
ALTER TABLE entregas DROP CONSTRAINT entregas_status_check;
ALTER TABLE entregas ADD CONSTRAINT entregas_status_check
    CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'AGUARDANDO', 'CONFIRMADA', 'ACEITA', 'PRONTO_PARA_ENTREGA', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADA'));

ALTER TABLE entregas ADD COLUMN pagamento_online BOOLEAN NOT NULL DEFAULT FALSE;
-- Segredo do link de pagamento do pedido (o cardápio público não tem login):
-- sem ele, ninguém consegue gerar/consultar pagamento de um pedido alheio
-- só chutando o id sequencial.
ALTER TABLE entregas ADD COLUMN pagamento_online_token VARCHAR(64);
ALTER TABLE entregas ADD COLUMN mp_payment_id VARCHAR(50);
ALTER TABLE entregas ADD COLUMN pagamento_online_status VARCHAR(30);

CREATE INDEX idx_entregas_aguardando_pagamento ON entregas (status, created_at) WHERE status = 'AGUARDANDO_PAGAMENTO';
