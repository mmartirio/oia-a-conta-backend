package com.oiaaconta.billing.exception;

// Erro vindo da API do Mercado Pago (cartão recusado na validação, token
// expirado, credencial inválida...) — a mensagem já é pra exibir ao dono.
public class MercadoPagoException extends RuntimeException {
    public MercadoPagoException(String mensagem) {
        super(mensagem);
    }
}
