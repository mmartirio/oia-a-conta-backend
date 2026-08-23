-- BUG PREEXISTENTE encontrado ao mexer nesse fluxo: o CHECK constraint de
-- "estado" (criado antes do Flyway existir nesse serviço, nunca atualizado)
-- não inclui vários estados que o enum EstadoSessao já usa há tempo
-- (AGUARDANDO_PEDIDO_WEB, COLETANDO_NUMERO_LID, PAUSADO já estavam faltando)
-- — salvar a sessão nesses estados falha silenciosamente (exceção engolida
-- pelo @Async), fazendo o cliente "esquecer" que já recebeu o link do
-- cardápio. Recriando o constraint com todos os estados atuais + o novo
-- COLETANDO_PEDIDO_CHAT (ver abaixo).
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
    'COLETANDO_PAGAMENTO',
    'COLETANDO_OBSERVACAO',
    'CONFIRMANDO_PEDIDO',
    'PEDIDO_ENVIADO',
    'AGUARDANDO_PIX',
    'PAUSADO'
));

-- Lembrete de 10 minutos sem finalizar o pedido: manda a imagem do cardápio
-- numerado (configurada pelo admin) + instruções, e não repete a cada
-- varredura do job (ver LembreteCardapioJob).
ALTER TABLE sessoes_whatsapp ADD COLUMN lembrete_cardapio_enviado BOOLEAN NOT NULL DEFAULT FALSE;

-- Imagem do cardápio numerado que o admin desenha e sobe manualmente
-- (data URI base64, mesmo padrão de imagem de produto no catalog-service).
ALTER TABLE whatsapp_config ADD COLUMN imagem_cardapio_base64 TEXT;
