-- Modalidade de operação escolhida no wizard de registro (só usada quando
-- o plano selecionado exige, ex: plano Startup) — repassada ao billing-service
-- na criação do contrato.
ALTER TABLE registros_pendentes ADD COLUMN modalidade_operacao VARCHAR(20);
