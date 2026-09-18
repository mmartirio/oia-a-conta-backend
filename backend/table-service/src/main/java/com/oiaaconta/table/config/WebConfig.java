package com.oiaaconta.table.config;

import com.oiaaconta.table.security.ModalidadeOperacaoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ModalidadeOperacaoInterceptor modalidadeOperacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(modalidadeOperacaoInterceptor).addPathPatterns("/api/mesas/**");
    }
}
