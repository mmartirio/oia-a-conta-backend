package com.oiaaconta.auth.dto.request;

import lombok.Data;

@Data
public class RestauranteCoresRequest {
    private String corPrimaria;
    private String corSecundaria;
    private String corAccent;
    private String corTexto;
}
