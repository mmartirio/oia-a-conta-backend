package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodItemResponse {
    private String id;
    private String externalCode;
}
