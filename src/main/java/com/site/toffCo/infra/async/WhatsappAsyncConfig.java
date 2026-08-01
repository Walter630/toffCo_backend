package com.site.toffCo.infra.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class WhatsappAsyncConfig {
    @Bean(name = "whatsappBotExecutor")
    public Executor whatsappBotExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8); executor.setMaxPoolSize(32); executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("whatsapp-bot-"); executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10); executor.initialize(); return executor;
    }

    @Bean(name = "n8nEventExecutor")
    public Executor n8nEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("n8n-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("Fila de eventos do n8n cheia; evento descartado"));
        executor.initialize();
        return executor;
    }
}
