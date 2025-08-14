package com.sysml.platform.api.graphql;

import com.sysml.platform.api.graphql.dataloader.RequirementDataLoader;
import com.sysml.platform.domain.requirements.RequirementDefinition;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataLoaderRegistrar;

/** DataLoader配置 - 解决GraphQL N+1查询问题 */
@Configuration
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "features.requirements", havingValue = "true", matchIfMissing = true)
public class DataLoaderConfig {

  private final RequirementDataLoader requirementDataLoader;

  @Bean
  public DataLoaderRegistrar dataLoaderRegistrar() {
    return (registry, context) -> {
      // 注册RequirementDataLoader
      DataLoader<String, RequirementDefinition> loader = requirementDataLoader.createDataLoader();
      registry.register("requirementLoader", loader);
    };
  }
}
