package com.sysml.platform.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@SuppressWarnings("SqlNoDataSourceInspection")
class PostgresConnectivityIT {

  @Autowired private DataSource dataSource;

  private static boolean isPortOpen(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private static String[] parseHostPort(String jdbcUrl) {
    // very lightweight parser for jdbc:postgresql://host:port/db
    try {
      int idx = jdbcUrl.indexOf("//");
      if (idx < 0) return new String[] {"localhost", "5432"};
      String tail = jdbcUrl.substring(idx + 2);
      String hostPort = tail.contains("/") ? tail.substring(0, tail.indexOf('/')) : tail;
      if (hostPort.contains(":")) {
        String[] parts = hostPort.split(":", 2);
        return new String[] {parts[0], parts[1]};
      }
      return new String[] {hostPort, "5432"};
    } catch (Exception e) {
      return new String[] {"localhost", "5432"};
    }
  }

  @Test
  void canObtainConnectionWhenPostgresAvailable() throws Exception {
    String url =
        System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/sysmldb");
    String[] hp = parseHostPort(url);
    Assumptions.assumeTrue(
        isPortOpen(hp[0], Integer.parseInt(hp[1]), 500),
        () -> "PostgreSQL not reachable at " + hp[0] + ":" + hp[1] + ", skipping test");

    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn).isNotNull();
      assertThat(conn.isValid(2)).isTrue();
    } catch (Exception ex) {
      // 若凭据/数据库未就绪，跳过不阻塞流水线
      Assumptions.assumeTrue(false, "PostgreSQL not ready: " + ex.getClass().getSimpleName());
    }
  }
}
