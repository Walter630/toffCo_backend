package com.site.toffCo.infra.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class WhatsappAsyncConfig {
    @Bean(name = "whatsappBotExecutor")
    public Executor whatsappBotExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8); executor.setMaxPoolSize(32); executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("whatsapp-bot-"); executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10); executor.initialize(); return executor;
    }
}
