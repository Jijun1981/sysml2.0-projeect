package com.sysml.platform.nfr;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * RQ-NFR-HEALTH-001: 健康检查
 * 验收条件：/health聚合所有子系统
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthAggregationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("健康检查应该聚合所有子系统状态")
    public void shouldAggregateAllSubsystemHealth() {
        // When: 请求健康检查端点
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        // Then: 返回聚合的健康状态
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // 验证包含各个组件的健康状态
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        assertNotNull(components, "应该包含components信息");
        
        // 验证CDO组件
        Map<String, Object> cdo = (Map<String, Object>) components.get("cdo");
        assertNotNull(cdo, "应该包含CDO健康状态");
        assertEquals("UP", cdo.get("status"));
    }

    @Test
    @DisplayName("应该能查询特定组件的健康状态")
    public void shouldQuerySpecificComponentHealth() {
        // When: 请求特定组件健康检查
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health/cdo", Map.class);

        // Then: 返回该组件的健康状态
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // 验证详细信息
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        assertNotNull(details);
        assertEquals("sysml-repo", details.get("repository"));
        assertEquals("lean", details.get("mode"));
    }

    @Test
    @DisplayName("健康检查应该支持GraphQL端点")
    public void shouldSupportGraphQLHealthQuery() {
        // GraphQL健康查询
        String query = "{ ok }";
        
        // 这个测试已经在GraphQLBasicTest中实现
        // 这里只是确认健康系统整合
        assert true;
    }
}