-- Gate de "aceitar/rejeitar pedido" pela cozinha para pedidos de cliente
-- (WhatsApp/cardápio): AGUARDANDO (cliente fez o pedido, ninguém decidiu
-- ainda) -> CONFIRMADA (cozinha aceitou, entra na produção e fica disponível
-- pra um entregador reivindicar) ou CANCELADA (cozinha rejeitou, com motivo).
-- O antigo fluxo de "aceitar" (entregador reivindicando a entrega) passa a
-- partir de CONFIRMADA em vez de AGUARDANDO.

ALTER TABLE entregas DROP CONSTRAINT entregas_status_check;

ALTER TABLE entregas ADD CONSTRAINT entregas_status_check
    CHECK (status IN ('AGUARDANDO', 'CONFIRMADA', 'ACEITA', 'PRONTO_PARA_ENTREGA', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADA'));

ALTER TABLE entregas ADD COLUMN motivo_rejeicao VARCHAR(500);
