package com.sysml.platform.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * RQ-UI-RUNTIME-001: 运行时绑定
 * 验收条件：/health/sirius返回UP
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SiriusRuntimeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Sirius运行时健康检查应该返回UP")
    public void shouldReturnSiriusHealthStatus() {
        // When: 请求Sirius健康检查端点
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health/sirius", Map.class);

        // Then: 返回状态应该是UP
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // 验证详细信息
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        assertNotNull(details);
        assertNotNull(details.get("runtime"), "应该包含运行时信息");
        assertEquals("CDO", details.get("backend"), "应该绑定CDO后端");
    }

    @Test
    @DisplayName("Sirius应该能连接到EMF模型")
    public void shouldConnectToEMFModels() {
        // When: 检查Sirius与EMF的连接
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/sirius/status", Map.class);

        // Then: 验证连接状态
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("emfConnected"), "EMF应该已连接");
        assertTrue((Boolean) body.get("modelsLoaded"), "模型应该已加载");
    }

    @Test
    @DisplayName("应该支持Sirius WebSocket连接")
    public void shouldSupportWebSocketConnection() {
        // 验证WebSocket端点可用
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/sirius/websocket/info", Map.class);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.get("endpoint"), "应该提供WebSocket端点");
        assertEquals("ws", ((String) body.get("endpoint")).substring(0, 2), "应该是WebSocket协议");
    }
}