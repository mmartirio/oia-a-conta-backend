package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 20)
    @Builder.Default
    private String plano = "BASICO";

    @Builder.Default
    private boolean ativo = true;

    @Column(name = "email_responsavel", length = 150)
    private String emailResponsavel;

    @Column(length = 18)
    private String cnpj;

    @Column(length = 20)
    private String telefone;

    @Column(name = "endereco_rua", length = 200)
    private String enderecoRua;

    @Column(name = "endereco_numero", length = 20)
    private String enderecoNumero;

    @Column(name = "endereco_bairro", length = 100)
    private String enderecoBairro;

    @Column(name = "endereco_cidade", length = 100)
    private String enderecoCidade;

    @Column(name = "endereco_complemento", length = 100)
    private String enderecoComplemento;

    // Ajuste manual da localização no mapa (arrastar o marcador no Dashboard)
    // — sobrepõe o endereço geocodificado automaticamente, usado quando a
    // geocodificação erra o ponto exato.
    private Double latitude;
    private Double longitude;

    @Column(name = "whatsapp_instance_name", unique = true, length = 100)
    private String whatsappInstanceName;

    // Data URI (ex: "data:image/png;base64,...") da logo própria do restaurante,
    // exibida no cardápio público no lugar da logo da plataforma.
    @Column(name = "logo_base64", columnDefinition = "TEXT")
    private String logoBase64;

    // Cores customizadas do cardápio público (hex, ex: "#CF4622"). Nulo = usa o
    // padrão da marca — existe para o admin poder evitar que a logo do
    // restaurante "suma" contra o fundo do header quando as cores coincidem.
    @Column(name = "cor_primaria", length = 9)
    private String corPrimaria;

    @Column(name = "cor_secundaria", length = 9)
    private String corSecundaria;

    @Column(name = "cor_accent", length = 9)
    private String corAccent;

    // Cor do texto do título (nome do restaurante) no header do cardápio público.
    @Column(name = "cor_texto", length = 9)
    private String corTexto;

    // Imagem de fundo do cardápio público (data URI) + opacidade (0-100, aplicada
    // só na imagem — o conteúdo/texto por cima continua 100% legível).
    @Column(name = "background_base64", columnDefinition = "TEXT")
    private String backgroundBase64;

    @Column(name = "background_opacidade")
    private Integer backgroundOpacidade;

    @Builder.Default
    private boolean bloqueado = false;

    @Column(name = "data_bloqueio")
    private LocalDateTime dataBloqueio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
