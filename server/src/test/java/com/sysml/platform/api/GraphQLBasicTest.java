package com.sysml.platform.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * RQ-API-ENDPOINT-001: 端点配置
 * 使用Spring GraphQL Test测试GraphQL端点
 */
@SpringBootTest
@AutoConfigureGraphQlTester
public class GraphQLBasicTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    @DisplayName("GraphQL端点应该响应查询")
    public void shouldRespondToQueries() {
        graphQlTester.document("{ ok }")
            .execute()
            .path("ok")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
    
    @Test
    @DisplayName("应该查询健康状态")
    public void shouldQueryHealth() {
        graphQlTester.document("{ health(component: \"api\") { status version } }")
            .execute()
            .path("health.status")
            .entity(String.class)
            .isEqualTo("UP");
    }
    
    @Test
    @DisplayName("支持introspection")
    public void shouldSupportIntrospection() {
        graphQlTester.document("{ __schema { types { name } } }")
            .execute()
            .path("__schema.types[*].name")
            .entityList(String.class)
            .contains("Query", "Mutation", "Health");
    }
}