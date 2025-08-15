package com.sysml.platform.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequirementsTDDIT {

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
  void createRequirement_shouldSucceed_and_queryList() {
    String m =
        "mutation{ createRequirement(input:{reqId:\"RQ-1\", name:\"First\"}) { ok error requirement { id reqId name } } }";
    ResponseEntity<String> resp = postGraphQL(m);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("\"ok\":true");
    assertThat(resp.getBody()).contains("\"reqId\":\"RQ-1\"");

    String q = "{ requirements(page:1,size:10){ total items { reqId name } } }";
    ResponseEntity<String> list = postGraphQL(q);
    assertThat(list.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(list.getBody()).contains("\"total\":1");
    assertThat(list.getBody()).contains("RQ-1");
  }
}
