package com.site.toffCo.module.whatzap.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;

@Component
public class WhatsappCircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }
    private final int failureThreshold;
    private final Duration openDuration;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;
    private boolean recoveryProbeInProgress;

    public WhatsappCircuitBreaker(@Value("${evolution.api.circuit.failure-threshold:5}") int failureThreshold,
                                  @Value("${evolution.api.circuit.open-duration:PT30S}") Duration openDuration) {
        if (failureThreshold < 1 || openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("Configuração inválida do circuit breaker");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }
    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) return true;
        if (state == State.HALF_OPEN) return !recoveryProbeInProgress;
        if (openedAt.plus(openDuration).isBefore(Instant.now())) {
            state = State.HALF_OPEN; recoveryProbeInProgress = true; return true;
        }
        return false;
    }
    public synchronized void recordSuccess() { state = State.CLOSED; consecutiveFailures = 0; openedAt = null; recoveryProbeInProgress = false; }
    public synchronized void recordFailure() { consecutiveFailures++; if (consecutiveFailures >= failureThreshold) { state = State.OPEN; openedAt = Instant.now(); recoveryProbeInProgress = false; } }
    public synchronized Snapshot snapshot() { return new Snapshot(state, consecutiveFailures, openedAt); }
    public record Snapshot(State state, int consecutiveFailures, Instant openedAt) {}
}
