package com.sysml.platform.api.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_test_db",
    "spring.datasource.username=sysml_user",
    "spring.datasource.password=sysml_password",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "cdo.store.drop-on-activate=true"
})
class HealthControllerIT {

  @LocalServerPort 
  int port;

  @Autowired
  TestRestTemplate rest;

  @Test
  void healthShouldReturnUp() {
    ResponseEntity<String> resp =
        rest.getForEntity("http://localhost:" + port + "/health", String.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("UP");
  }
}