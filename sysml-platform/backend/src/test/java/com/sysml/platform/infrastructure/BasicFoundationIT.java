package com.sysml.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** 基础Foundation Phase验证 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_dev_db",
      "spring.datasource.username=postgres",
      "spring.datasource.password=123456"
    })
public class BasicFoundationIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {
    assertNotNull(jdbcTemplate);
  }

  @Test
  void postgreSQLShouldBeConnected() {
    String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
    assertThat(version).contains("PostgreSQL");
    System.out.println("PostgreSQL version: " + version);
  }
}
