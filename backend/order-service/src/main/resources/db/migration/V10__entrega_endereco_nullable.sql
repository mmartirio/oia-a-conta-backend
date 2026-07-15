-- O restante do app trata o endereço de entrega como um único campo de texto
-- livre (endereco_rua) — é assim que o fluxo do chatbot já preenche (número/
-- bairro/cidade ficam null de propósito). O schema real (pg_dump) tinha essas
-- duas colunas como NOT NULL, o que quebrava esse fluxo com violação de
-- constraint (silenciosamente engolida como "erro ao enviar o pedido" para o
-- cliente). Alinha o schema com o que o DTO (EntregaRequest) já validava.
ALTER TABLE entregas ALTER COLUMN endereco_numero DROP NOT NULL;
ALTER TABLE entregas ALTER COLUMN endereco_cidade DROP NOT NULL;
