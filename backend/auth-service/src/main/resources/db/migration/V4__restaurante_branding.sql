-- Branding do cardápio público por restaurante: logo própria (data URI em
-- base64) e cores customizadas do header, para o admin poder evitar que a
-- logo "suma" quando as cores da marca coincidem com o fundo do header.
ALTER TABLE restaurantes ADD COLUMN logo_base64 TEXT;
ALTER TABLE restaurantes ADD COLUMN cor_primaria VARCHAR(9);
ALTER TABLE restaurantes ADD COLUMN cor_secundaria VARCHAR(9);
ALTER TABLE restaurantes ADD COLUMN cor_accent VARCHAR(9);
