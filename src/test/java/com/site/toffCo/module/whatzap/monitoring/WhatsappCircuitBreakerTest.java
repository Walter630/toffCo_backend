package com.site.toffCo.module.whatzap.monitoring;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class WhatsappCircuitBreakerTest {
    @Test
    void abreAposLimiteERecusaNovasTentativas() {
        WhatsappCircuitBreaker breaker = new WhatsappCircuitBreaker(2, Duration.ofSeconds(30));
        breaker.recordFailure(); breaker.recordFailure();
        assertEquals(WhatsappCircuitBreaker.State.OPEN, breaker.snapshot().state());
        assertFalse(breaker.allowRequest());
    }

    @Test
    void recuperaComUmaSondaDepoisDoTempoAberto() throws InterruptedException {
        WhatsappCircuitBreaker breaker = new WhatsappCircuitBreaker(1, Duration.ofMillis(1));
        breaker.recordFailure(); Thread.sleep(5);
        assertTrue(breaker.allowRequest());
        assertEquals(WhatsappCircuitBreaker.State.HALF_OPEN, breaker.snapshot().state());
        assertFalse(breaker.allowRequest());
        breaker.recordSuccess();
        assertEquals(WhatsappCircuitBreaker.State.CLOSED, breaker.snapshot().state());
    }
}
