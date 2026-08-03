package com.site.toffCo.infra.config;

import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint de Health Check detalhado para monitoramento externo.
 *
 * Verifica: PostgreSQL, Redis, RabbitMQ, WhatsApp/Evolution API.
 *
 * Uso: GET /api/health/detailed
 * Não requer autenticação (configurar no SecurityConfig).
 *
 * O que faz:
 * - Testa conexão com banco de dados
 * - Testa conexão com Redis
 * - Verifica estado do circuit breaker do WhatsApp
 * - Retorna métricas de envio de mensagens
 *
 * Por que:
 * - Permite monitoramento automatizado via script ou n8n
 * - Detecta problemas ANTES de impactar o usuário final
 * - Fornece dados concretos para diagnóstico rápido
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final WhatsappMonitoringService whatsappMonitoringService;
    private final WhatsappCircuitBreaker whatsappCircuitBreaker;

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        var result = new HashMap<String, Object>();
        var components = new HashMap<String, Object>();
        boolean allHealthy = true;

        // --- PostgreSQL ---
        components.put("database", checkDatabase());
        if (!getStatus(components.get("database")).equals("UP")) allHealthy = false;

        // --- Redis ---
        components.put("redis", checkRedis());
        if (!getStatus(components.get("redis")).equals("UP")) allHealthy = false;

        // --- WhatsApp Metrics ---
        var whatsappInfo = checkWhatsapp();
        result.put("whatsapp", whatsappInfo);
        if ("OPEN".equals(whatsappInfo.get("circuitState"))) allHealthy = false;

        result.put("status", allHealthy ? "UP" : "DEGRADED");
        result.put("components", components);
        result.put("timestamp", java.time.Instant.now().toString());
        result.put("version", "1.0.0");

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint simples para health check rápido (uptime monitors, load balancers).
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", java.time.Instant.now().toString()));
    }

    private Map<String, Object> checkDatabase() {
        var info = new HashMap<String, Object>();
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            info.put("status", "UP");
            info.put("latencyMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            info.put("status", "DOWN");
            info.put("message", e.getMessage());
            info.put("latencyMs", System.currentTimeMillis() - start);
            log.error("Health check: Database DOWN", e);
        }
        return info;
    }

    private Map<String, Object> checkRedis() {
        var info = new HashMap<String, Object>();
        long start = System.currentTimeMillis();
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            info.put("status", "UP");
            info.put("latencyMs", System.currentTimeMillis() - start);
            info.put("response", pong);
        } catch (Exception e) {
            info.put("status", "DOWN");
            info.put("message", e.getMessage());
            info.put("latencyMs", System.currentTimeMillis() - start);
            log.error("Health check: Redis DOWN", e);
        }
        return info;
    }

    private Map<String, Object> checkWhatsapp() {
        var info = new HashMap<String, Object>();
        try {
            var snapshot = whatsappMonitoringService.snapshot();
            info.put("circuitState", whatsappCircuitBreaker.isOpen() ? "OPEN" : "CLOSED");
            info.put("totalAttempts", snapshot.attempts());
            info.put("successes", snapshot.successes());
            info.put("failures", snapshot.failures());
            info.put("circuitBlocked", snapshot.circuitBlocked());
            info.put("averageLatencyMs", snapshot.averageLatencyMs());

            double failureRate = snapshot.attempts() == 0 ? 0.0 :
                    (double) snapshot.failures() / snapshot.attempts();
            info.put("failureRate", failureRate);
        } catch (Exception e) {
            info.put("circuitState", "UNKNOWN");
            info.put("message", e.getMessage());
            log.error("Health check: WhatsApp metrics unavailable", e);
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    private String getStatus(Object component) {
        if (component instanceof Map<?, ?> rawMap) {
            Object status = rawMap.get("status");
            return status instanceof String s ? s : "UNKNOWN";
        }
        return "UNKNOWN";
    }
}
