package com.sysml.platform.config;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

  @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/sysml_dev_db}")
  private String dbUrl;

  @Value("${spring.datasource.username:postgres}")
  private String dbUser;

  @Value("${spring.datasource.password:}")
  private String dbPassword;

  @Bean
  @Profile({"dev", "test"})
  public DataSource devAndTestDataSource() {
    // 对于测试环境，使用Spring配置的值
    return createPostgresDataSource(dbUrl, dbUser, dbPassword);
  }

  @Bean
  @Profile("prod")
  public DataSource prodDataSource() {
    // 生产环境从环境变量读取
    return createPostgresDataSource(
        System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysmldb"),
        System.getenv().getOrDefault("DATABASE_USER", "sysml"),
        System.getenv("DATABASE_PASSWORD"));
  }

  private static DataSource createPostgresDataSource(String url, String user, String password) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(url);
    ds.setUser(user);
    if (password != null && !password.isEmpty()) {
      ds.setPassword(password);
    }
    return ds;
  }
}
