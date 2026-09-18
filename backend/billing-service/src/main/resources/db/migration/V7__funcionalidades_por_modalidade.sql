-- Recursos exibidos no seletor Presencial/Delivery do card do plano — só
-- usados quando o plano exige modalidade (exige_modalidade_operacao=true).
-- Planos sem essa restrição continuam usando a coluna "funcionalidades".
ALTER TABLE planos ADD COLUMN funcionalidades_mesas TEXT;
ALTER TABLE planos ADD COLUMN funcionalidades_delivery TEXT;
