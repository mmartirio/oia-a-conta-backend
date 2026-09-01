-- Fluxo de escolha de sabor: cliente pediu um combo com grupos (ver
-- migration V14 do catalog-service) e o bot está perguntando, grupo por
-- grupo, quais sabores ele quer. Precisa lembrar em qual combo/grupo a
-- conversa está no meio de várias mensagens.
ALTER TABLE sessoes_whatsapp ADD COLUMN combo_selecao_id BIGINT;
ALTER TABLE sessoes_whatsapp ADD COLUMN combo_selecao_grupo_index INTEGER;

ALTER TABLE sessoes_whatsapp DROP CONSTRAINT IF EXISTS sessoes_whatsapp_estado_check;
ALTER TABLE sessoes_whatsapp ADD CONSTRAINT sessoes_whatsapp_estado_check CHECK (estado IN (
    'INICIO',
    'COLETANDO_NUMERO_LID',
    'COLETANDO_NOME',
    'COLETANDO_ENDERECO_RUA',
    'COLETANDO_ENDERECO_NUMERO',
    'COLETANDO_ENDERECO_BAIRRO',
    'COLETANDO_ENDERECO_CIDADE',
    'COLETANDO_ENDERECO_COMPLEMENTO',
    'COLETANDO_ENDERECO',
    'NAVEGANDO_CATEGORIAS',
    'NAVEGANDO_PRODUTOS',
    'AGUARDANDO_QUANTIDADE',
    'REVISANDO_CARRINHO',
    'AGUARDANDO_PEDIDO_WEB',
    'COLETANDO_PEDIDO_CHAT',
    'ESCOLHENDO_SABORES_COMBO',
    'COLETANDO_PAGAMENTO',
    'COLETANDO_OBSERVACAO',
    'CONFIRMANDO_PEDIDO',
    'PEDIDO_ENVIADO',
    'AGUARDANDO_PIX',
    'PAUSADO'
));

-- Sabores escolhidos pro item, quando ele é um combo — formato simples
-- "produtoId:quantidade;produtoId:quantidade" (não é JSON de verdade,
-- não precisa ser), acumulado conforme cada grupo do combo é respondido.
-- Lido ao montar o pedido pro order-service (ver ChatbotService.enviarPedido).
ALTER TABLE itens_carrinho ADD COLUMN sabores_escolhidos TEXT;
