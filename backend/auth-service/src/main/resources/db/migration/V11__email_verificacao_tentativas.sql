-- Limita tentativas de adivinhar o código de verificação de 6 dígitos —
-- sem isso, o código (válido por 15 min, endpoint público) é totalmente
-- brute-forceável (1 milhão de combinações, sem nenhum limite).
ALTER TABLE email_verificacoes ADD COLUMN tentativas INTEGER NOT NULL DEFAULT 0;
