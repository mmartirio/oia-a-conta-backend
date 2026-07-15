-- Campos de endereço do restaurante (edição de dados da empresa pelo admin).
ALTER TABLE restaurantes
    ADD COLUMN endereco_rua VARCHAR(200),
    ADD COLUMN endereco_numero VARCHAR(20),
    ADD COLUMN endereco_bairro VARCHAR(100),
    ADD COLUMN endereco_cidade VARCHAR(100),
    ADD COLUMN endereco_complemento VARCHAR(100);
