-- Foto ilustrativa do produto, salva como data URI base64 (mesmo padrão da logo do restaurante em auth-service).
ALTER TABLE produtos ADD COLUMN imagem_base64 TEXT;
