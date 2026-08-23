package com.oiaaconta.ifood.dto.ifood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfoodCategoriaRequest {
    private String name;
    private String externalCode;
}
