package com.sysml.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 数据库连接测试 */
@SpringBootTest(classes = {com.sysml.platform.config.DataSourceConfig.class})
@ActiveProfiles("test")
public class DatabaseConnectionTest {

  @Autowired private DataSource dataSource;

  @Test
  void testDatabaseConnection() throws Exception {
    assertThat(dataSource).isNotNull();

    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn).isNotNull();
      assertThat(conn.isValid(5)).isTrue();

      // 验证数据库信息
      var metaData = conn.getMetaData();
      System.out.println("Database: " + metaData.getDatabaseProductName());
      System.out.println("Version: " + metaData.getDatabaseProductVersion());
      System.out.println("URL: " + metaData.getURL());
      System.out.println("User: " + metaData.getUserName());
    }
  }
}
