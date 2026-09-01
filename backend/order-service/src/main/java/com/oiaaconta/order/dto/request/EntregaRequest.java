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

    @NotBlank(message = "Endereço obrigatório")
    private String enderecoRua;

    private String enderecoNumero;

    private String enderecoBairro;

    private String enderecoCidade;

    private String enderecoComplemento;

    // Geocodificados no frontend a partir do endereço acima — opcionais: se
    // vier nulo (endereço não encontrado, API de geocoding fora do ar), o
    // pedido segue normalmente e só o frete fica sem cálculo.
    private Double enderecoLatitude;
    private Double enderecoLongitude;

    @NotNull(message = "Método de pagamento obrigatório")
    private MetodoPagamento metodoPagamento;

    @Min(1) @Max(12)
    private Integer parcelas;

    private String observacao;

    private boolean origemWhatsapp;

    private boolean origemPdv;

    private boolean origemIfood;

    // ID do pedido no iFood — obrigatório sse origemIfood, validado em EntregaService.
    private String ifoodOrderId;

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    private List<ItemRequest> itens;

    @Data
    public static class ItemRequest {
        // Obrigatório sse comboId não for informado — validado em EntregaService.
        private Long produtoId;
        private String produtoNome;
        @NotNull @Min(1) private Integer quantidade;
        private BigDecimal precoUnitario;
        private String observacao;

        // Preenchidos quando este item representa um Combo — nesse caso
        // produtoId/produtoNome/precoUnitario são ignorados: EntregaService
        // expande o combo em itens reais buscando a composição no catalog-service.
        private Long comboId;
        private Integer comboQuantidade;

        // Sabores escolhidos pelo cliente dentro de cada grupo do combo (ver
        // EscolhaSaborRequest) — vazio/nulo = usa o primeiro produto elegível
        // de cada grupo como padrão.
        private List<EscolhaSaborRequest> saboresEscolhidos;
    }
}
