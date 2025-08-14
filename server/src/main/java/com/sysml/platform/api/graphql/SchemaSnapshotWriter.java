package com.sysml.platform.api.graphql;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "features.graphql.schemaSnapshot", havingValue = "true", matchIfMissing = true)
public class SchemaSnapshotWriter {

  private final GraphQlSource graphQlSource;

  public SchemaSnapshotWriter(GraphQlSource graphQlSource) {
    this.graphQlSource = graphQlSource;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void writeSchemaToBuildDir() {
    try {
      GraphQLSchema schema = graphQlSource.schema();
      String sdl =
          new SchemaPrinter(
                  SchemaPrinter.Options.defaultOptions()
                      .includeScalarTypes(true)
                      .includeIntrospectionTypes(false))
              .print(schema);
      Path out = Path.of("build", "generated", "schema.graphql");
      Files.createDirectories(out.getParent());
      Files.writeString(out, sdl);
    } catch (IOException ignored) {
    }
  }
}
