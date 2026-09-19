-- Limite de trocas gratuitas de modalidade (2x) por contrato — a partir da
-- 3ª, a troca só pode ser feita pelo suporte e acumula uma cobrança de
-- R$30,00 por troca, somada automaticamente no próximo pagamento manual
-- registrado pra esse contrato (ver BillingService.registrarPagamentoManual).
ALTER TABLE contratos ADD COLUMN trocas_modalidade_gratis_usadas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE contratos ADD COLUMN saldo_encargos_modalidade NUMERIC(10,2) NOT NULL DEFAULT 0;
