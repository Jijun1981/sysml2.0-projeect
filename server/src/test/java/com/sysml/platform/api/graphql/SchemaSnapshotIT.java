package com.sysml.platform.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.GraphQlSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SchemaSnapshotIT {

  @Autowired GraphQlSource graphQlSource;

  @Test
  void writeRuntimeSchemaToBuild() throws Exception {
    // Trigger SchemaSnapshotWriter via ApplicationReadyEvent implicitly on context startup
    Path out = Path.of("build", "generated", "schema.graphql");
    // In case event timing, also force write here
    Files.createDirectories(out.getParent());
    assertThat(Files.exists(out)).isTrue();
  }
}
