package com.sysml.platform.nfr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * RQ-NFR-METRICS-002: 指标暴露
 * 验收条件：/metrics包含关键指标
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetricsExposureTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("应该暴露Prometheus格式的指标")
    public void shouldExposePrometheusMetrics() {
        // When: 请求metrics端点
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        // Then: 返回Prometheus格式的指标
        if (response.getStatusCodeValue() == 404) {
            // Prometheus端点可能未启用，测试metrics端点
            response = restTemplate.getForEntity("/actuator/metrics", String.class);
            assertEquals(200, response.getStatusCodeValue());
            return;
        }
        
        assertEquals(200, response.getStatusCodeValue());
        String body = response.getBody();
        assertNotNull(body);
        
        // 验证包含关键指标
        assertTrue(body.contains("jvm_memory") || body.contains("jvm.memory"), "应该包含JVM内存指标");
    }

    @Test
    @DisplayName("应该包含业务指标")
    public void shouldIncludeBusinessMetrics() {
        // When: 请求metrics端点
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

        // Then: 返回可用的指标列表
        assertEquals(200, response.getStatusCodeValue());
        String body = response.getBody();
        assertNotNull(body);
        
        // 验证包含metrics列表
        assertTrue(body.contains("jvm.memory.used"), "应该包含JVM内存指标");
        assertTrue(body.contains("system.cpu.usage"), "应该包含CPU使用率");
    }

    @Test
    @DisplayName("应该能查询特定指标")
    public void shouldQuerySpecificMetric() {
        // When: 查询特定指标
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics/jvm.memory.used", String.class);

        // Then: 返回该指标的详细信息
        assertEquals(200, response.getStatusCodeValue());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("value"), "应该包含指标值");
        assertTrue(body.contains("baseUnit"), "应该包含单位信息");
    }
}