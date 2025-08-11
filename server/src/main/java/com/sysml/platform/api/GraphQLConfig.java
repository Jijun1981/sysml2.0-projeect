package com.sysml.platform.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;

/**
 * RQ-API-ENDPOINT-001: GraphQL端点配置
 * 配置GraphQL运行时，包括scalar类型和wiring
 */
@Configuration
public class GraphQLConfig {

    /**
     * 配置GraphQL运行时wiring
     * 包括自定义scalar类型和其他配置
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Json);
    }
}