-- O código de verificação de e-mail passa a ser guardado só como hash
-- SHA-256 (64 caracteres hex). Códigos pendentes (válidos por 15 min) são
-- descartados — quem estiver no meio do cadastro pede "reenviar código".
DELETE FROM email_verificacoes;
ALTER TABLE email_verificacoes ALTER COLUMN codigo TYPE VARCHAR(64);
