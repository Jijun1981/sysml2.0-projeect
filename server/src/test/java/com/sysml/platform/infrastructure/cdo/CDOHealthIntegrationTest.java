package com.sysml.platform.infrastructure.cdo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * RQ-INFRA-CDO-001: CDO健康与配置
 * 集成测试：验证actuator端点
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CDOHealthIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void cdoHealthShouldBeAvailableInActuator() {
        // When: 请求actuator健康端点
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);
        
        // Then: CDO组件应该存在且状态为UP
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        assertNotNull(components);
        
        Map<String, Object> cdo = (Map<String, Object>) components.get("cdo");
        assertNotNull(cdo);
        assertEquals("UP", cdo.get("status"));
        
        Map<String, Object> details = (Map<String, Object>) cdo.get("details");
        assertEquals("sysml-repo", details.get("repository"));
        assertEquals("lean", details.get("mode"));
    }
}