package com.oiaaconta.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @EnableAsync sem um TaskExecutor customizado usa o SimpleAsyncTaskExecutor
 * padrão do Spring, que cria uma thread nova (sem limite) a cada chamada
 * @Async. Este bean, nomeado "taskExecutor", é detectado automaticamente
 * pelo processamento de @Async do Spring e substitui esse comportamento por
 * um pool limitado.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("auth-async-");
        executor.initialize();
        return executor;
    }
}
