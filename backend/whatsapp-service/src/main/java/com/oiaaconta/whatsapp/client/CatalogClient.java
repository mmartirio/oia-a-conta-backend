package com.oiaaconta.whatsapp.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

// Cardápio numerado do chatbot: número que o admin desenhou na imagem →
// produtoId real. Endpoint público (sem Authorization) porque o chatbot fala
// com clientes anônimos, mesmo padrão de OrderClient/combos públicos.
@FeignClient(name = "catalog-service", path = "/api")
public interface CatalogClient {

    @GetMapping("/catalog/publico/{restauranteId}/produtos-numerados")
    List<ProdutoNumeradoResponse> listarProdutosNumerados(@PathVariable("restauranteId") Long restauranteId);

    @Data
    class ProdutoNumeradoResponse {
        private Integer numero;
        private Long produtoId;
        private Long comboId;
        private String nome;
        private BigDecimal preco;
    }
}
