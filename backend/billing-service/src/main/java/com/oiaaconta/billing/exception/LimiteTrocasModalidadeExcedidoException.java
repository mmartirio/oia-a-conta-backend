package com.oiaaconta.billing.exception;

// Lançada quando o dono do restaurante tenta trocar a modalidade de
// operação (mesas/delivery) sozinho depois de já ter usado as trocas
// gratuitas — a partir daí só o suporte pode fazer a troca (e ela passa a
// ser cobrada). Ver BillingService.alterarMinhaModalidade.
public class LimiteTrocasModalidadeExcedidoException extends RuntimeException {
    public LimiteTrocasModalidadeExcedidoException(String message) {
        super(message);
    }
}
