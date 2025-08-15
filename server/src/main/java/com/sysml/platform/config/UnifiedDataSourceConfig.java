package com.sysml.platform.config;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 统一的数据源配置 优先级： 1. spring.datasource.* 属性 2. DATABASE_* 环境变量 3. 默认值 */
@Configuration
public class UnifiedDataSourceConfig {

  @Value("${spring.datasource.url:#{null}}")
  private String springUrl;

  @Value("${spring.datasource.username:#{null}}")
  private String springUsername;

  @Value("${spring.datasource.password:#{null}}")
  private String springPassword;

  @Value("${DATABASE_HOST:localhost}")
  private String dbHost;

  @Value("${DATABASE_PORT:5432}")
  private int dbPort;

  @Value("${DATABASE_NAME:sysml_db}")
  private String dbName;

  @Value("${DATABASE_USER:sysml_user}")
  private String dbUser;

  @Value("${DATABASE_PASSWORD:sysml_password}")
  private String dbPassword;

  @Bean
  @Primary
  @ConditionalOnProperty(name = "datasource.unified", havingValue = "true", matchIfMissing = true)
  public DataSource unifiedDataSource() {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();

    // 优先使用spring.datasource配置
    if (springUrl != null && !springUrl.isEmpty()) {
      // 解析JDBC URL
      String url = springUrl.replace("jdbc:postgresql://", "");
      String[] parts = url.split("/");
      if (parts.length >= 2) {
        String[] hostPort = parts[0].split(":");
        dataSource.setServerNames(new String[] {hostPort[0]});
        if (hostPort.length > 1) {
          dataSource.setPortNumbers(new int[] {Integer.parseInt(hostPort[1])});
        }
        dataSource.setDatabaseName(parts[1].split("\\?")[0]);
      }

      if (springUsername != null) {
        dataSource.setUser(springUsername);
      }
      if (springPassword != null) {
        dataSource.setPassword(springPassword);
      }
    } else {
      // 使用DATABASE_*环境变量
      dataSource.setServerNames(new String[] {dbHost});
      dataSource.setPortNumbers(new int[] {dbPort});
      dataSource.setDatabaseName(dbName);
      dataSource.setUser(dbUser);
      dataSource.setPassword(dbPassword);
    }

    // 通用配置
    dataSource.setCurrentSchema("public");
    dataSource.setConnectTimeout(10);
    dataSource.setSocketTimeout(5);
    dataSource.setTcpKeepAlive(true);
    dataSource.setPrepareThreshold(5);

    logDataSourceConfig(dataSource);

    return dataSource;
  }

  private void logDataSourceConfig(PGSimpleDataSource dataSource) {
    System.out.println("=== Unified DataSource Configuration ===");
    System.out.println("Host: " + String.join(",", dataSource.getServerNames()));
    System.out.println("Port: " + dataSource.getPortNumbers()[0]);
    System.out.println("Database: " + dataSource.getDatabaseName());
    System.out.println("User: " + dataSource.getUser());
    System.out.println("Schema: " + dataSource.getCurrentSchema());
    System.out.println("========================================");
  }
}
