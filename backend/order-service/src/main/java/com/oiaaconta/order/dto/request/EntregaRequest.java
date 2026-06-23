package com.oiaaconta.order.dto.request;

import com.oiaaconta.order.enums.MetodoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EntregaRequest {

    @NotBlank(message = "Nome do cliente obrigatório")
    private String clienteNome;

    private String clienteTelefone;

    @NotBlank(message = "Rua obrigatória")
    private String enderecoRua;

    @NotBlank(message = "Número obrigatório")
    private String enderecoNumero;

    private String enderecoBairro;

    @NotBlank(message = "Cidade obrigatória")
    private String enderecoCidade;

    private String enderecoComplemento;

    @NotNull(message = "Método de pagamento obrigatório")
    private MetodoPagamento metodoPagamento;

    @Min(1) @Max(12)
    private Integer parcelas;

    private String observacao;

    private boolean origemWhatsapp;

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    private List<ItemRequest> itens;

    @Data
    public static class ItemRequest {
        @NotNull private Long produtoId;
        @NotBlank private String produtoNome;
        @NotNull @Min(1) private Integer quantidade;
        @NotNull private BigDecimal precoUnitario;
        private String observacao;
    }
}
