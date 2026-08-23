package com.oiaaconta.auth.service;

import java.util.Set;

// Catálogo de permissões concedíveis a um Grupo — um item por container do
// sidebar; WHATSAPP_* é uma por tab; CONFIG_* é uma por item dentro de
// Configurações. Mantido em sincronia com o filtro do api-gateway
// (Part B) e a derivação de authorities em cada microsserviço (Part A).
public final class Permissao {

    private Permissao() { }

    public static final Set<String> CHAVES = Set.of(
        "DASHBOARD",
        "CARDAPIO",
        "MESAS",
        "COZINHA",
        "COMANDA",
        "GARCOM",
        "DELIVERY",
        "CAIXA_PDV",
        "ENTREGADOR",
        "USUARIOS",
        "CLIENTES",
        "ESTOQUE",
        "MARKETING",
        "FINANCEIRO",
        "WHATSAPP_CONEXAO",
        "WHATSAPP_MENSAGENS",
        "WHATSAPP_CONVERSAS",
        "SUPORTE",
        "CONFIG_STATUS_LOJA",
        "CONFIG_ALERTA_PEDIDO",
        "CONFIG_DADOS_EMPRESA",
        "CONFIG_PIX",
        "CONFIG_COMISSOES",
        "CONFIG_LOGO",
        "CONFIG_CORES",
        "CONFIG_BACKGROUND",
        "CONFIG_HORARIOS",
        "CONFIG_PAUSAS",
        "CONFIG_FRETE",
        "IFOOD_CONEXAO"
    );
}
