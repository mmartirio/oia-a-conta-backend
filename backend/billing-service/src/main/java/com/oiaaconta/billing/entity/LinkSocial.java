package com.oiaaconta.billing.entity;

import com.oiaaconta.billing.enums.TipoLinkSocial;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "links_sociais")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLinkSocial tipo;

    @Column(nullable = false, length = 500)
    private String url;

    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
