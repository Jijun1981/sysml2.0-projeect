package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.util.*;
import java.time.Duration;
import java.time.Instant;

/**
 * RQ-REQ-QUERY-004: 查询性能
 * 验收条件：
 * - 中等数据集P50<200ms
 * - 大数据集P50<500ms
 * 
 * @TestCase TC-REQ-QUERY-001
 * @TestCase TC-REQ-QUERY-002
 * @TestCase TC-REQ-QUERY-003
 */
public class RequirementQueryPerformanceTest {
    
    private RequirementService service;
    
    @BeforeEach
    public void setUp() {
        service = new RequirementService();
    }
    
    /**
     * TC-REQ-QUERY-001: 基本查询功能
     */
    @Test
    @DisplayName("应该支持按关键字查询需求")
    public void shouldQueryRequirementsByKeyword() {
        // Given: 创建测试数据
        createTestRequirement("REQ-001", "User Authentication", "User must login with email");
        createTestRequirement("REQ-002", "Password Reset", "User can reset password");
        createTestRequirement("REQ-003", "Session Management", "System manages user sessions");
        
        // When: 按关键字查询
        QueryRequirementsPayload result = service.queryRequirements(
            QueryRequirementsInput.builder()
                .keyword("user")
                .build()
        );
        
        // Then: 应该返回包含关键字的需求
        assertTrue(result.isOk());
        assertEquals(3, result.getTotalElements());
        assertNotNull(result.getContent());
    }
    
    /**
     * TC-REQ-QUERY-002: 中等数据集性能测试 (1000条)
     */
    @Test
    @DisplayName("中等数据集查询P50应该小于200ms")
    public void mediumDatasetPerformanceTest() {
        // Given: 创建1000条需求
        int dataSize = 1000;
        for (int i = 0; i < dataSize; i++) {
            createTestRequirement(
                String.format("REQ-%04d", i),
                String.format("Requirement %d", i),
                String.format("Description for requirement %d", i)
            );
        }
        
        // When: 执行多次查询，收集响应时间
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();
            
            QueryRequirementsPayload result = service.queryRequirements(
                QueryRequirementsInput.builder()
                    .keyword("requirement")
                    .page(i % 10)
                    .size(20)
                    .build()
            );
            
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            responseTimes.add(duration);
            
            assertTrue(result.isOk());
        }
        
        // Then: 计算P50
        Collections.sort(responseTimes);
        long p50 = responseTimes.get(responseTimes.size() / 2);
        
        System.out.println("Medium dataset P50: " + p50 + "ms");
        assertTrue(p50 < 200, "P50 should be less than 200ms, but was " + p50 + "ms");
    }
    
    /**
     * TC-REQ-QUERY-003: 大数据集性能测试 (10000条)
     */
    @Test
    @DisplayName("大数据集查询P50应该小于500ms")
    public void largeDatasetPerformanceTest() {
        // Given: 创建10000条需求
        int dataSize = 10000;
        for (int i = 0; i < dataSize; i++) {
            createTestRequirement(
                String.format("REQ-%05d", i),
                String.format("Requirement %d", i),
                String.format("Description for requirement %d with more text", i)
            );
        }
        
        // When: 执行多次查询
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();
            
            QueryRequirementsPayload result = service.queryRequirements(
                QueryRequirementsInput.builder()
                    .keyword("requirement")
                    .page(i % 50)
                    .size(20)
                    .build()
            );
            
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            responseTimes.add(duration);
            
            assertTrue(result.isOk());
        }
        
        // Then: 计算P50
        Collections.sort(responseTimes);
        long p50 = responseTimes.get(responseTimes.size() / 2);
        
        System.out.println("Large dataset P50: " + p50 + "ms");
        assertTrue(p50 < 500, "P50 should be less than 500ms, but was " + p50 + "ms");
    }
    
    private void createTestRequirement(String reqId, String name, String text) {
        service.createRequirement(
            CreateRequirementInput.builder()
                .reqId(reqId)
                .name(name)
                .text(text)
                .kind(RequirementKind.FUNCTIONAL)
                .build()
        );
    }
}