-- Respostas automáticas por palavra-chave: quando o cliente manda o texto do
-- "acionador" (case-insensitive), o bot responde com "mensagem" sem alterar
-- o estado da conversa em andamento.
CREATE TABLE automacoes_mensagem (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    acionador VARCHAR(200) NOT NULL,
    mensagem TEXT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_automacoes_mensagem_restaurante ON automacoes_mensagem (restaurante_id);
