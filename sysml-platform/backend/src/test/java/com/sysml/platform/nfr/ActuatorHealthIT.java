package com.sysml.platform.nfr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthIT {

  @LocalServerPort int port;

  TestRestTemplate rest = new TestRestTemplate();

  @Test
  void actuatorHealthShouldBeUpAndExposeCdo() {
    ResponseEntity<String> resp =
        rest.getForEntity("http://localhost:" + port + "/actuator/health", String.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("\"status\":\"UP\"");
    assertThat(resp.getBody()).contains("cdo");
  }
}
