package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * RQ-REQ-CRUD-001: CRUD操作
 * 验收条件：必填验证、reqId唯一
 */
public class RequirementServiceTest {
    
    private RequirementService service;
    
    @BeforeEach
    public void setUp() {
        service = new RequirementService();
    }
    
    @Test
    public void shouldCreateRequirement() {
        // Given: 创建需求输入
        CreateRequirementInput input = CreateRequirementInput.builder()
            .reqId("REQ-001")
            .name("System shall provide login")
            .text("The system shall provide user login capability")
            .kind(RequirementKind.FUNCTIONAL)
            .priority(RequirementPriority.HIGH)
            .build();
        
        // When: 创建需求
        CreateRequirementPayload result = service.createRequirement(input);
        
        // Then: 应该成功创建
        assertTrue(result.isOk());
        assertNull(result.getError());
        assertNotNull(result.getRequirement());
        assertEquals("REQ-001", result.getRequirement().getReqId());
        assertEquals("System shall provide login", result.getRequirement().getName());
    }
    
    @Test
    public void shouldRejectDuplicateReqId() {
        // Given: 创建第一个需求
        CreateRequirementInput input1 = CreateRequirementInput.builder()
            .reqId("REQ-001")
            .name("First requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        service.createRequirement(input1);
        
        // When: 尝试用相同reqId创建第二个需求
        CreateRequirementInput input2 = CreateRequirementInput.builder()
            .reqId("REQ-001")
            .name("Second requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementPayload result = service.createRequirement(input2);
        
        // Then: 应该失败
        assertFalse(result.isOk());
        assertNotNull(result.getError());
        assertEquals("REQ_ID_DUPLICATE", result.getError().getCode());
    }
    
    @Test
    public void shouldValidateRequiredFields() {
        // Given: 缺少必填字段的输入
        CreateRequirementInput input = CreateRequirementInput.builder()
            .reqId("REQ-002")
            // 缺少name和kind
            .build();
        
        // When: 尝试创建
        CreateRequirementPayload result = service.createRequirement(input);
        
        // Then: 应该失败
        assertFalse(result.isOk());
        assertNotNull(result.getError());
        assertEquals("VALIDATION_ERROR", result.getError().getCode());
    }
}