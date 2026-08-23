-- Pedidos de delivery deixam de gerar uma comanda "virtual" (mesa 0) —
-- comanda é exclusiva do fluxo de garçom/mesa. comanda_id vira opcional.
ALTER TABLE pedidos ALTER COLUMN comanda_id DROP NOT NULL;
