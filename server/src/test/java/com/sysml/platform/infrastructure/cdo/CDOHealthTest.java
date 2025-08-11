package com.sysml.platform.infrastructure.cdo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/** RQ-INFRA-CDO-001: CDO健康与配置 验收条件：GET /health/cdo返回UP */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CDOHealthTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void shouldReturnCDOHealthStatus() {
    // When: 请求CDO健康检查端点
    ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health/cdo", Map.class);

    // Then: 返回状态应该是UP
    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
  }
}
