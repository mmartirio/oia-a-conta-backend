-- Modalidade de operação (mesas x delivery), restrita a planos que exigem
-- essa escolha no cadastro (ex: plano Startup). Nos demais planos o
-- contrato fica com modalidade_operacao NULL e ambas ficam liberadas.

ALTER TABLE planos ADD COLUMN exige_modalidade_operacao BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE contratos ADD COLUMN modalidade_operacao VARCHAR(20);
ALTER TABLE contratos ADD CONSTRAINT chk_contratos_modalidade_operacao
    CHECK (modalidade_operacao IN ('MESAS', 'DELIVERY'));

-- Limite de atendentes de WhatsApp — null = sem limite. Qual coluna vale
-- depende da modalidade do contrato (ver Plano.java/BillingService).
ALTER TABLE planos ADD COLUMN limite_atendentes_whatsapp INTEGER;
ALTER TABLE planos ADD COLUMN limite_atendentes_whatsapp_mesas INTEGER;
ALTER TABLE planos ADD COLUMN limite_atendentes_whatsapp_delivery INTEGER;
