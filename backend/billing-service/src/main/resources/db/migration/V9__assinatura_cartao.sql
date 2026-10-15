-- Plano de teste da cobrança recorrente no cartão (Mercado Pago Assinaturas):
-- R$ 0,50 cobrado na hora (sem período de teste), pra validar o fluxo de
-- ponta a ponta com dinheiro de verdade. Depois do teste, desativar pelo
-- Gestor (Planos → ativo) — não apagar, pode haver contrato apontando pra ele.
INSERT INTO planos (nome, descricao, preco_mensal, limite_usuarios, limite_mesas, funcionalidades,
                    periodo_teste, dias_teste, ativo, destaque, exige_modalidade_operacao, created_at)
VALUES ('Teste', 'Plano de teste da cobrança no cartão — cobra R$ 0,50 na hora.', 0.50, 10, 20,
        'Cardápio digital,Delivery,Cozinha em tempo real,Controle de mesas,Atendimento de garçom,PDV / Caixa,Gestão de usuários,Relatórios financeiros,WhatsApp integrado,Painel de entregadores',
        false, 0, true, false, false, now());

-- O webhook do Mercado Pago reenvia a mesma notificação várias vezes
-- (retentativas, recycling da cobrança) — o índice único garante que um
-- pagamento do MP vira um Pagamento só.
CREATE UNIQUE INDEX uk_pagamentos_mp_payment_id ON pagamentos (mp_payment_id) WHERE mp_payment_id IS NOT NULL;
