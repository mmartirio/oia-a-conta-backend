-- Trilha de auditoria de negócio, por restaurante (tenant), pro painel Gestor.
-- Cada linha é um evento relevante (login, pedido criado, pagamento
-- confirmado, configuração alterada, usuário criado/removido, etc.) que outro
-- microserviço registra aqui via chamada interna (POST /internal/auditoria).
CREATE TABLE logs_auditoria (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    usuario_id BIGINT,
    usuario_nome VARCHAR(100),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_logs_auditoria_restaurante ON logs_auditoria (restaurante_id, criado_em DESC);
