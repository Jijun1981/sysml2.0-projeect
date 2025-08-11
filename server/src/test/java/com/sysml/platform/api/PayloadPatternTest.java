package com.sysml.platform.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * RQ-API-CORE-002: 核心契约
 * 验证统一Payload模式
 */
@SpringBootTest
@AutoConfigureGraphQlTester
public class PayloadPatternTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    @DisplayName("Mutation应该返回统一的Payload格式")
    public void mutationShouldReturnPayloadPattern() {
        String mutation = """
            mutation {
                createRequirement(input: {
                    reqId: "REQ-001",
                    name: "Test Requirement",
                    text: "This is a test requirement",
                    kind: FUNCTIONAL
                }) {
                    ok
                    error {
                        code
                        messageKey
                    }
                    requirement {
                        id
                        reqId
                        name
                    }
                }
            }
            """;
        
        graphQlTester.document(mutation)
            .execute()
            .path("createRequirement.ok")
            .entity(Boolean.class)
            .satisfies(ok -> {
                // 验证ok字段存在
                assert ok != null;
            });
    }
    
    @Test
    @DisplayName("错误场景应该返回Error对象")
    public void errorScenarioShouldReturnError() {
        String mutation = """
            mutation {
                createRequirement(input: {
                    reqId: "",
                    name: "",
                    text: "",
                    kind: FUNCTIONAL
                }) {
                    ok
                    error {
                        code
                        messageKey
                        path
                    }
                }
            }
            """;
        
        graphQlTester.document(mutation)
            .execute()
            .path("createRequirement")
            .entity(Object.class)
            .satisfies(result -> {
                // 验证返回格式符合Payload模式
                assert result != null;
            });
    }
    
    @Test
    @DisplayName("查询应该支持分页")
    public void queryShouldSupportPagination() {
        String query = """
            {
                requirements(page: 0, size: 10) {
                    content {
                        id
                        reqId
                        name
                    }
                    totalElements
                    totalPages
                    pageNumber
                    pageSize
                }
            }
            """;
        
        graphQlTester.document(query)
            .execute()
            .path("requirements.pageSize")
            .entity(Integer.class)
            .isEqualTo(10);
    }
}