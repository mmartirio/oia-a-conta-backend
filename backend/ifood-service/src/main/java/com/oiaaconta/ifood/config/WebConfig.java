package com.oiaaconta.ifood.config;

import com.oiaaconta.ifood.security.RestricoesOperacaoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Lazy no PARÂMETRO do construtor (não dá pra usar @RequiredArgsConstructor
// do Lombok aqui — ele não copia @Lazy do campo pro parâmetro gerado, e sem
// isso o boot falha: o interceptor depende do BillingClient (Feign), e a
// criação de clients Feign depende do WebMvcAutoConfiguration, que por sua
// vez coleta todo WebMvcConfigurer (este WebConfig) — fechando um ciclo.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RestricoesOperacaoInterceptor restricoesOperacaoInterceptor;

    public WebConfig(@Lazy RestricoesOperacaoInterceptor restricoesOperacaoInterceptor) {
        this.restricoesOperacaoInterceptor = restricoesOperacaoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restricoesOperacaoInterceptor).addPathPatterns("/api/ifood/admin/**");
    }
}
