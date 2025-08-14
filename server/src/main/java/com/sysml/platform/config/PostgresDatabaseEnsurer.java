package com.sysml.platform.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.postgresql.Driver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class PostgresDatabaseEnsurer {

  @PostConstruct
  public void ensureDatabaseExists() {
    String url =
        System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysmldb");
    String user = System.getenv().getOrDefault("DATABASE_USER", "sysml");
    String pwd = System.getenv("DATABASE_PASSWORD");

    String targetDb = extractDbName(url);
    String hostPort = extractHostPort(url);
    String adminUrl = "jdbc:postgresql://" + hostPort + "/postgres";
    try {
      DriverManager.registerDriver(new Driver());
      try (Connection conn = DriverManager.getConnection(adminUrl, user, pwd)) {
        try (Statement st = conn.createStatement()) {
          ResultSet rs =
              st.executeQuery("SELECT 1 FROM pg_database WHERE datname='" + targetDb + "'");
          boolean exists = rs.next();
          if (!exists) {
            st.executeUpdate("CREATE DATABASE \"" + targetDb + "\"");
          }
        }
      }
    } catch (Exception ignore) {
      // If we cannot ensure DB creation (insufficient privileges, etc.), leave it to operator
    }
  }

  private static String extractDbName(String jdbcUrl) {
    int idx = jdbcUrl.lastIndexOf('/');
    if (idx > 0 && idx < jdbcUrl.length() - 1) {
      return jdbcUrl.substring(idx + 1);
    }
    return "sysmldb";
  }

  private static String extractHostPort(String jdbcUrl) {
    int idx = jdbcUrl.indexOf("//");
    if (idx < 0) return "localhost:5432";
    String tail = jdbcUrl.substring(idx + 2);
    int slash = tail.indexOf('/');
    return slash > 0 ? tail.substring(0, slash) : tail;
  }
}

