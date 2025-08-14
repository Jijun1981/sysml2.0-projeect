package com.sysml.platform.config;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

  @Bean
  @Profile("dev")
  public DataSource devDataSource() {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setServerNames(new String[]{"localhost"});
    dataSource.setPortNumbers(new int[]{5432});
    dataSource.setDatabaseName("sysml_dev_db");
    dataSource.setUser("postgres");
    dataSource.setPassword("123456");
    return dataSource;
  }

  @Bean
  @Profile("prod")
  public DataSource prodDataSource() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(
        System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysmldb"));
    ds.setUser(System.getenv().getOrDefault("DATABASE_USER", "sysml"));
    String pwd = System.getenv("DATABASE_PASSWORD");
    if (pwd != null) {
      ds.setPassword(pwd);
    }
    return ds;
  }
}
