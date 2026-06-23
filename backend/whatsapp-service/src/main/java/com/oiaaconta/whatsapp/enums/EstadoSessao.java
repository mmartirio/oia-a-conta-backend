package com.oiaaconta.whatsapp.enums;

public enum EstadoSessao {
    INICIO,
    COLETANDO_NOME,
    // estados legados de endereço fracionado — mantidos para não quebrar dados existentes no DB
    COLETANDO_ENDERECO_RUA,
    COLETANDO_ENDERECO_NUMERO,
    COLETANDO_ENDERECO_BAIRRO,
    COLETANDO_ENDERECO_CIDADE,
    COLETANDO_ENDERECO_COMPLEMENTO,
    // novo estado único de endereço
    COLETANDO_ENDERECO,
    NAVEGANDO_CATEGORIAS,
    NAVEGANDO_PRODUTOS,
    AGUARDANDO_QUANTIDADE,
    REVISANDO_CARRINHO,
    COLETANDO_PAGAMENTO,
    COLETANDO_OBSERVACAO,
    CONFIRMANDO_PEDIDO,
    PEDIDO_ENVIADO,
    AGUARDANDO_PIX
}
