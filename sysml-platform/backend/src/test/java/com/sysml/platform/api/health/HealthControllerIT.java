package com.sysml.platform.api.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerIT {

  @LocalServerPort int port;

  TestRestTemplate rest = new TestRestTemplate();

  @Test
  void healthShouldReturnUp() {
    ResponseEntity<String> resp =
        rest.getForEntity("http://localhost:" + port + "/health", String.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("UP");
  }
}
