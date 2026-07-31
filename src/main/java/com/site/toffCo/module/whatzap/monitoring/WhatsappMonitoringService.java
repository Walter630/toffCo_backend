package com.site.toffCo.module.whatzap.monitoring;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WhatsappMonitoringService {
    private static final String KEY_PREFIX = "toffco:whatsapp:metrics:";
    private final StringRedisTemplate redisTemplate;
    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong circuitBlocked = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    public WhatsappMonitoringService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    public long startTimer() { increment("attempts", attempts); return System.nanoTime(); }
    public void recordSuccess(long startedAt) { increment("successes", successes); addLatency(elapsedMs(startedAt)); }
    public void recordFailure(long startedAt) { increment("failures", failures); addLatency(elapsedMs(startedAt)); }
    public void recordCircuitBlocked() { increment("circuitBlocked", circuitBlocked); }

    public Snapshot snapshot() {
        long a = read("attempts", attempts);
        long latency = read("latencyMs", totalLatencyMs);
        return new Snapshot(a, read("successes", successes), read("failures", failures),
                read("circuitBlocked", circuitBlocked), a == 0 ? 0 : latency / a);
    }

    private long elapsedMs(long startedAt) { return (System.nanoTime() - startedAt) / 1_000_000; }
    private void increment(String name, AtomicLong fallback) {
        fallback.incrementAndGet();
        try { redisTemplate.opsForValue().increment(KEY_PREFIX + name); }
        catch (RuntimeException ignored) { }
    }
    private void addLatency(long latency) {
        totalLatencyMs.addAndGet(latency);
        try { redisTemplate.opsForValue().increment(KEY_PREFIX + "latencyMs", latency); }
        catch (RuntimeException ignored) { }
    }
    private long read(String name, AtomicLong fallback) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + name);
            return value == null ? fallback.get() : Long.parseLong(value);
        } catch (RuntimeException ignored) { return fallback.get(); }
    }
    public record Snapshot(long attempts, long successes, long failures, long circuitBlocked, long averageLatencyMs) {}
}
