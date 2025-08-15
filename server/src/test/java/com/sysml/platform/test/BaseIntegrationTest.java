package com.sysml.platform.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 基础集成测试类 - 统一使用Testcontainers管理PostgreSQL */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:14")
          .withDatabaseName("sysml_test_db")
          .withUsername("sysml_user")
          .withPassword("sysml_password")
          .withInitScript("init-test.sql"); // 可选的初始化脚本

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    // 动态设置数据源属性，覆盖所有配置
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // 也设置环境变量格式的配置
    registry.add("DATABASE_HOST", postgres::getHost);
    registry.add(
        "DATABASE_PORT", () -> postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
    registry.add("DATABASE_NAME", postgres::getDatabaseName);
    registry.add("DATABASE_USER", postgres::getUsername);
    registry.add("DATABASE_PASSWORD", postgres::getPassword);

    // CDO相关配置
    registry.add("cdo.enabled", () -> true);
    registry.add("cdo.repository.name", () -> "sysml-test");
    registry.add("cdo.server.port", () -> 2037);
    registry.add("cdo.store.drop-on-activate", () -> true);
  }

  @BeforeAll
  static void beforeAll() {
    // 确保容器已启动
    postgres.start();
  }

  /** 获取测试数据库连接信息 */
  protected static String getJdbcUrl() {
    return postgres.getJdbcUrl();
  }

  protected static String getUsername() {
    return postgres.getUsername();
  }

  protected static String getPassword() {
    return postgres.getPassword();
  }
}
