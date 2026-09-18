package com.oiaaconta.whatsapp.config;

import com.oiaaconta.whatsapp.security.RestricoesOperacaoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RestricoesOperacaoInterceptor restricoesOperacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restricoesOperacaoInterceptor).addPathPatterns("/api/whatsapp/admin/**");
    }
}
