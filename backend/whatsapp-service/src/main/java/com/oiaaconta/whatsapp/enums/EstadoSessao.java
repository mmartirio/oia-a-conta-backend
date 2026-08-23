package com.oiaaconta.whatsapp.enums;

public enum EstadoSessao {
    INICIO,
    // só usado para contatos "@lid" sem número real ainda salvo — pergunta o
    // WhatsApp do cliente antes de seguir com o atendimento normal
    COLETANDO_NUMERO_LID,
    COLETANDO_NOME,
    // estados legados de endereço fracionado — mantidos para não quebrar dados existentes no DB
    COLETANDO_ENDERECO_RUA,
    COLETANDO_ENDERECO_NUMERO,
    COLETANDO_ENDERECO_BAIRRO,
    COLETANDO_ENDERECO_CIDADE,
    COLETANDO_ENDERECO_COMPLEMENTO,
    // novo estado único de endereço
    COLETANDO_ENDERECO,
    // aguardando o cliente finalizar o pedido pelo cardápio digital (link enviado)
    AGUARDANDO_PEDIDO_WEB,
    // depois do lembrete de 10 min (imagem do cardápio numerado): aguardando o
    // cliente responder com números/nomes de produtos direto no chat
    COLETANDO_PEDIDO_CHAT,
    COLETANDO_PAGAMENTO,
    COLETANDO_OBSERVACAO,
    CONFIRMANDO_PEDIDO,
    PEDIDO_ENVIADO,
    AGUARDANDO_PIX,
    PAUSADO
}
