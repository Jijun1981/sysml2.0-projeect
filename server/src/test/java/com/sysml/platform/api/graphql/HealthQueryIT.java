package com.sysml.platform.api.graphql;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test-nodb")
@TestPropertySource(properties = {
  "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.graphql.data.GraphQlQueryByExampleAutoConfiguration",
  "spring.graphql.data.query-by-example.enabled=false",
  "spring.data.jpa.repositories.enabled=false",
  "CDO_ENABLED=false",
  "features.requirements=false",
  "features.graphql.schemaSnapshot=false"
})
class HealthQueryIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void okShouldReturnTrue() throws Exception {
    String query = "{\"query\":\"{ ok }\"}";
    mockMvc
        .perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.ok").value(true));
  }

  @Test
  void healthShouldContainComponents() throws Exception {
    String query = "{\"query\":\"{ health { status components } }\"}";
    mockMvc
        .perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(query))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.health.status").value(anyOf(is("UP"), is("DOWN"), is("UNKNOWN"))))
        .andExpect(jsonPath("$.data.health.components").exists());
  }

  @Test
  void healthComponentsShouldIncludeCdoAndDb() throws Exception {
    String query = "{\"query\":\"{ health { components } }\"}";
    mockMvc
        .perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.health.components.cdo").exists())
        .andExpect(jsonPath("$.data.health.components.db").exists());
  }
}
