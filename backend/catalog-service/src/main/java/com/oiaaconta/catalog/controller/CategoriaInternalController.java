package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Endpoint "/internal/**" — sem JWT, confiável só por estar dentro da rede
// Docker interna (mesmo padrão do InternalContratoController do billing-service).
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class CategoriaInternalController {

    private final CategoriaService categoriaService;

    @PostMapping("/categorias/padrao")
    public ResponseEntity<Void> criarCategoriasPadrao(@RequestBody Map<String, Long> body) {
        categoriaService.criarCategoriasPadrao(body.get("restauranteId"));
        return ResponseEntity.noContent().build();
    }
}
