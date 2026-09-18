package com.oiaaconta.table.config;

import com.oiaaconta.table.security.ModalidadeOperacaoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Lazy é essencial aqui: o interceptor depende do BillingClient (Feign),
// e a criação de clients Feign depende do WebMvcAutoConfiguration — que
// por sua vez coleta todo WebMvcConfigurer (este WebConfig) pra registrar
// interceptors. Sem @Lazy isso forma um ciclo e o boot falha com
// UnsatisfiedDependencyException / "circular reference".
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Lazy
    private final ModalidadeOperacaoInterceptor modalidadeOperacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(modalidadeOperacaoInterceptor).addPathPatterns("/api/mesas/**");
    }
}
