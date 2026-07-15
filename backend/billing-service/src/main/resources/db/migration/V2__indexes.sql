-- Índices em colunas usadas para filtrar por tenant e por status.
-- contratos.restaurante_id já tem um índice implícito via a unique
-- constraint uk_contratos_restaurante_id, então não duplicamos aqui.

CREATE INDEX idx_tickets_suporte_restaurante_id ON tickets_suporte (restaurante_id);
CREATE INDEX idx_tickets_suporte_status ON tickets_suporte (status);
CREATE INDEX idx_pagamentos_contrato_id ON pagamentos (contrato_id);
CREATE INDEX idx_mensagens_ticket_ticket_id ON mensagens_ticket (ticket_id);
