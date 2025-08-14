package com.sysml.platform.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQLApiIT {

  @LocalServerPort int port;

  TestRestTemplate rest = new TestRestTemplate();

  private ResponseEntity<String> postGraphQL(String query) {
    String url = "http://localhost:" + port + "/graphql";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"query\":\"" + query.replace("\"", "\\\"") + "\"}";
    return rest.postForEntity(url, new HttpEntity<>(body, headers), String.class);
  }

  @Test
  void okShouldReturnTrue() {
    ResponseEntity<String> resp = postGraphQL("{ ok }");
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("\"ok\":true");
  }

  @Test
  void healthShouldReturnUpAndComponents() {
    ResponseEntity<String> resp = postGraphQL("{ health { status components } }");
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("\"status\":\"UP\"");
    assertThat(resp.getBody()).contains("cdo").contains("sirius").contains("database");
  }
}
