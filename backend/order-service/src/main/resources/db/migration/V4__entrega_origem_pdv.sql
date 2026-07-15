-- Rastreabilidade de entregas criadas pelo PDV (balcão), usada para
-- restringir PIX nesse canal (EntregaService.criar).

ALTER TABLE entregas ADD COLUMN origem_pdv BOOLEAN NOT NULL DEFAULT FALSE;
