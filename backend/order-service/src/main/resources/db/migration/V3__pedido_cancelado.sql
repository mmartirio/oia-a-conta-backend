-- Adiciona CANCELADO ao enum de status de pedido (StatusPedido).
-- A baseline real (pg_dump) tem um CHECK constraint em pedidos.status que
-- so permite ENVIADO/PREPARANDO/PRONTO/ENTREGUE; sem este ALTER, qualquer
-- INSERT/UPDATE com status=CANCELADO falha com violacao de constraint.

ALTER TABLE pedidos DROP CONSTRAINT pedidos_status_check;

ALTER TABLE pedidos ADD CONSTRAINT pedidos_status_check
    CHECK (status IN ('ENVIADO', 'PREPARANDO', 'PRONTO', 'ENTREGUE', 'CANCELADO'));
