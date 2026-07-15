-- Imagem de fundo do cardápio público (data URI) + opacidade (0-100).
ALTER TABLE restaurantes ADD COLUMN background_base64 TEXT;
ALTER TABLE restaurantes ADD COLUMN background_opacidade INTEGER;
