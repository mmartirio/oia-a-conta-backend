package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    // Preço por unidade (tipoPreco=UNIDADE) ou preço por quilo (tipoPreco=PESO)
    // — mesmo campo nos dois casos, o que muda é a interpretação na hora de
    // calcular o subtotal do item (ver order-service, que recebe o peso
    // vendido em gramas e divide por 1000 pra multiplicar por este valor).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_preco", nullable = false, length = 20)
    @Builder.Default
    private TipoPreco tipoPreco = TipoPreco.UNIDADE;

    // Foto ilustrativa do produto: data URI completa (ex: "data:image/jpeg;base64,...") ou null.
    @Column(name = "imagem_base64", columnDefinition = "TEXT")
    private String imagemBase64;

    // Número desse produto na imagem do cardápio numerado que o admin desenha
    // pra enviar no chatbot do WhatsApp — nulo pra produtos que não aparecem
    // nessa imagem. Não tem relação com a ordem de exibição do cardápio
    // público (essa é só por categoria.ordem).
    @Column(name = "numero_cardapio")
    private Integer numeroCardapio;

    // Tempo de preparo próprio (minutos) — nulo usa o padrão do restaurante
    // (ver RestauranteConfig.tempoPreparoPadraoMinutos, order-service).
    @Column(name = "tempo_preparo_minutos")
    private Integer tempoPreparoMinutos;

    // Regra opcional de tempo por quantidade: a partir de
    // tempoPreparoQuantidadeLimite unidades no pedido, usa
    // tempoPreparoMinutosAcimaLimite em vez de tempoPreparoMinutos. Os dois
    // campos são usados juntos ou nenhum dos dois.
    @Column(name = "tempo_preparo_quantidade_limite")
    private Integer tempoPreparoQuantidadeLimite;

    @Column(name = "tempo_preparo_minutos_acima_limite")
    private Integer tempoPreparoMinutosAcimaLimite;

    @Builder.Default
    private boolean ativo = true;
}
