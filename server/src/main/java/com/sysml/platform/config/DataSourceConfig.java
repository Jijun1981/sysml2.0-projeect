package com.sysml.platform.config;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

  @Bean
  @Profile({"dev", "test"})
  public DataSource devAndTestDataSource() {
    return createPostgresDataSource(
        System.getenv()
            .getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysml_dev_db"),
        System.getenv().getOrDefault("DATABASE_USER", "postgres"),
        System.getenv("DATABASE_PASSWORD"));
  }

  @Bean
  @Profile("prod")
  public DataSource prodDataSource() {
    return createPostgresDataSource(
        System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysmldb"),
        System.getenv().getOrDefault("DATABASE_USER", "sysml"),
        System.getenv("DATABASE_PASSWORD"));
  }

  private static DataSource createPostgresDataSource(String url, String user, String password) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(url);
    ds.setUser(user);
    if (password != null) {
      ds.setPassword(password);
    }
    return ds;
  }
}
