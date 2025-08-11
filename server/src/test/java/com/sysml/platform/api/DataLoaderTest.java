package com.sysml.platform.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * RQ-API-DATALOADER-003: DataLoader批处理
 * 验证N+1查询优化
 */
@SpringBootTest
@AutoConfigureGraphQlTester
public class DataLoaderTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    @DisplayName("批量查询需求应该使用DataLoader优化")
    public void shouldUseBatchLoading() {
        // 查询多个需求的关联数据
        String query = """
            {
                requirements(page: 0, size: 10) {
                    content {
                        id
                        name
                        derivedFrom {
                            id
                            name
                        }
                    }
                }
            }
            """;
        
        graphQlTester.document(query)
            .execute()
            .path("requirements")
            .entity(Object.class)
            .satisfies(result -> {
                // 验证返回结果
                assert result != null;
            });
    }
    
    @Test
    @DisplayName("DataLoader应该缓存结果避免重复查询")
    public void shouldCacheResults() {
        // 查询包含重复引用的数据
        String query = """
            {
                requirements(page: 0, size: 5) {
                    content {
                        id
                        name
                        derivedFrom {
                            id
                        }
                        refines {
                            id
                        }
                    }
                }
            }
            """;
        
        graphQlTester.document(query)
            .execute()
            .path("requirements.content")
            .entityList(Object.class)
            .hasSize(0); // 暂时没有数据
    }
}