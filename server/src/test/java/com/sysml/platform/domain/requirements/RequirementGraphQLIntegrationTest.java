package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;

/**
 * RQ-REQ-CRUD-001: CRUD操作
 * 验收条件：GraphQL Mutation成功
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequirementGraphQLIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GraphQL createRequirement Mutation应该成功")
    public void graphqlCreateRequirementShouldSucceed() {
        // Given: GraphQL mutation请求
        String mutation = """
            mutation {
                createRequirement(input: {
                    reqId: "REQ-GQL-001"
                    name: "GraphQL测试需求"
                    text: "通过GraphQL创建的需求"
                    kind: FUNCTIONAL
                    priority: HIGH
                }) {
                    ok
                    requirement {
                        id
                        reqId
                        name
                        kind
                        priority
                        status
                    }
                    error {
                        code
                        messageKey
                    }
                }
            }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = Map.of("query", mutation);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // When: 发送GraphQL请求
        ResponseEntity<Map> response = restTemplate.postForEntity("/graphql", request, Map.class);

        // Then: 应该成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.get("data"));
        
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> createResult = (Map<String, Object>) data.get("createRequirement");
        
        assertTrue((Boolean) createResult.get("ok"));
        assertNull(createResult.get("error"));
        
        Map<String, Object> requirement = (Map<String, Object>) createResult.get("requirement");
        assertNotNull(requirement);
        assertEquals("REQ-GQL-001", requirement.get("reqId"));
        assertEquals("GraphQL测试需求", requirement.get("name"));
        assertEquals("FUNCTIONAL", requirement.get("kind"));
        assertEquals("HIGH", requirement.get("priority"));
        assertEquals("DRAFT", requirement.get("status"));
    }

    @Test
    @DisplayName("GraphQL requirements查询应该返回分页结果")
    public void graphqlRequirementsQueryShouldReturnPage() {
        // Given: 先创建一个需求
        String createMutation = """
            mutation {
                createRequirement(input: {
                    reqId: "REQ-QUERY-001"
                    name: "查询测试需求"
                    kind: FUNCTIONAL
                }) {
                    ok
                }
            }
        """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/graphql", 
            new HttpEntity<>(Map.of("query", createMutation), headers), 
            Map.class);

        // When: 查询需求列表
        String query = """
            {
                requirements(page: 0, size: 10) {
                    content {
                        reqId
                        name
                    }
                    totalElements
                    pageNumber
                    pageSize
                }
            }
        """;
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("query", query), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("/graphql", request, Map.class);

        // Then: 应该返回分页结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.get("data"));
        
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> requirements = (Map<String, Object>) data.get("requirements");
        
        assertNotNull(requirements);
        assertNotNull(requirements.get("content"));
        assertEquals(0, requirements.get("pageNumber"));
        assertEquals(10, requirements.get("pageSize"));
    }

    @Test
    @DisplayName("GraphQL deriveRequirement Mutation应该成功建立关系")
    public void graphqlDeriveRequirementShouldSucceed() {
        // Given: 创建两个需求
        String createParent = """
            mutation {
                createRequirement(input: {
                    reqId: "REQ-PARENT-001"
                    name: "父需求"
                    kind: FUNCTIONAL
                }) {
                    requirement { id }
                }
            }
        """;
        
        String createChild = """
            mutation {
                createRequirement(input: {
                    reqId: "REQ-CHILD-001"
                    name: "子需求"
                    kind: FUNCTIONAL
                }) {
                    requirement { id }
                }
            }
        """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<Map> parentResponse = restTemplate.postForEntity("/graphql",
            new HttpEntity<>(Map.of("query", createParent), headers), Map.class);
        ResponseEntity<Map> childResponse = restTemplate.postForEntity("/graphql",
            new HttpEntity<>(Map.of("query", createChild), headers), Map.class);
        
        Map<String, Object> parentData = (Map<String, Object>) parentResponse.getBody().get("data");
        Map<String, Object> parentResult = (Map<String, Object>) parentData.get("createRequirement");
        Map<String, Object> parentReq = (Map<String, Object>) parentResult.get("requirement");
        String parentId = (String) parentReq.get("id");
        
        Map<String, Object> childData = (Map<String, Object>) childResponse.getBody().get("data");
        Map<String, Object> childResult = (Map<String, Object>) childData.get("createRequirement");
        Map<String, Object> childReq = (Map<String, Object>) childResult.get("requirement");
        String childId = (String) childReq.get("id");
        
        // When: 建立derive关系
        String deriveMutation = String.format("""
            mutation {
                deriveRequirement(sourceId: "%s", targetId: "%s") {
                    ok
                    source { reqId }
                    target { reqId }
                    error { code }
                }
            }
        """, childId, parentId);
        
        ResponseEntity<Map> deriveResponse = restTemplate.postForEntity("/graphql",
            new HttpEntity<>(Map.of("query", deriveMutation), headers), Map.class);
        
        // Then: 应该成功
        assertEquals(HttpStatus.OK, deriveResponse.getStatusCode());
        
        Map<String, Object> deriveData = (Map<String, Object>) deriveResponse.getBody().get("data");
        Map<String, Object> deriveResult = (Map<String, Object>) deriveData.get("deriveRequirement");
        
        assertTrue((Boolean) deriveResult.get("ok"));
        assertNull(deriveResult.get("error"));
        
        Map<String, Object> source = (Map<String, Object>) deriveResult.get("source");
        Map<String, Object> target = (Map<String, Object>) deriveResult.get("target");
        
        assertEquals("REQ-CHILD-001", source.get("reqId"));
        assertEquals("REQ-PARENT-001", target.get("reqId"));
    }
}