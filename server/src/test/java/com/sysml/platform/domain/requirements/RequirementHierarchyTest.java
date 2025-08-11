package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * RQ-REQ-HIERARCHY-003: 层次DAG
 * 验收条件：环检测返回REQ_CYCLE_DETECTED
 * 集成测试：三层闭环测试失败
 * 
 * @TestCase TC-REQ-HIERARCHY-001
 * @TestCase TC-REQ-HIERARCHY-002
 * @TestCase TC-REQ-HIERARCHY-003
 */
public class RequirementHierarchyTest {
    
    private RequirementService service;
    
    @BeforeEach
    public void setUp() {
        service = new RequirementService();
    }
    
    /**
     * TC-REQ-HIERARCHY-001: 正常的derive关系建立
     */
    @Test
    public void shouldCreateDeriveRelationship() {
        // Given: 创建两个需求
        CreateRequirementInput parent = CreateRequirementInput.builder()
            .reqId("REQ-001")
            .name("Parent Requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput child = CreateRequirementInput.builder()
            .reqId("REQ-002")
            .name("Child Requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        
        CreateRequirementPayload parentResult = service.createRequirement(parent);
        CreateRequirementPayload childResult = service.createRequirement(child);
        
        // When: 建立derive关系
        DeriveRequirementPayload deriveResult = service.deriveRequirement(
            childResult.getRequirement().getId(),
            parentResult.getRequirement().getId()
        );
        
        // Then: 关系应该建立成功
        assertTrue(deriveResult.isOk());
        assertNull(deriveResult.getError());
        assertEquals("REQ-002", deriveResult.getSource().getReqId());
        assertEquals("REQ-001", deriveResult.getTarget().getReqId());
    }
    
    /**
     * TC-REQ-HIERARCHY-002: 检测两层环
     */
    @Test
    public void shouldDetectTwoLevelCycle() {
        // Given: 创建两个需求并建立A->B关系
        CreateRequirementInput reqA = CreateRequirementInput.builder()
            .reqId("REQ-A")
            .name("Requirement A")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput reqB = CreateRequirementInput.builder()
            .reqId("REQ-B")
            .name("Requirement B")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        
        CreateRequirementPayload resultA = service.createRequirement(reqA);
        CreateRequirementPayload resultB = service.createRequirement(reqB);
        
        service.deriveRequirement(
            resultA.getRequirement().getId(),
            resultB.getRequirement().getId()
        );
        
        // When: 尝试建立B->A关系（形成环）
        DeriveRequirementPayload cycleResult = service.deriveRequirement(
            resultB.getRequirement().getId(),
            resultA.getRequirement().getId()
        );
        
        // Then: 应该检测到环
        assertFalse(cycleResult.isOk());
        assertNotNull(cycleResult.getError());
        assertEquals("REQ_CYCLE_DETECTED", cycleResult.getError().getCode());
    }
    
    /**
     * TC-REQ-HIERARCHY-003: 检测三层环
     */
    @Test
    public void shouldDetectThreeLevelCycle() {
        // Given: 创建三个需求并建立A->B->C关系链
        CreateRequirementInput reqA = CreateRequirementInput.builder()
            .reqId("REQ-A")
            .name("Requirement A")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput reqB = CreateRequirementInput.builder()
            .reqId("REQ-B")
            .name("Requirement B")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput reqC = CreateRequirementInput.builder()
            .reqId("REQ-C")
            .name("Requirement C")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        
        CreateRequirementPayload resultA = service.createRequirement(reqA);
        CreateRequirementPayload resultB = service.createRequirement(reqB);
        CreateRequirementPayload resultC = service.createRequirement(reqC);
        
        // A derives from B
        service.deriveRequirement(
            resultA.getRequirement().getId(),
            resultB.getRequirement().getId()
        );
        
        // B derives from C
        service.deriveRequirement(
            resultB.getRequirement().getId(),
            resultC.getRequirement().getId()
        );
        
        // When: 尝试C derives from A（形成三层环）
        DeriveRequirementPayload cycleResult = service.deriveRequirement(
            resultC.getRequirement().getId(),
            resultA.getRequirement().getId()
        );
        
        // Then: 应该检测到环
        assertFalse(cycleResult.isOk());
        assertNotNull(cycleResult.getError());
        assertEquals("REQ_CYCLE_DETECTED", cycleResult.getError().getCode());
        assertEquals("error.req.cycle", cycleResult.getError().getMessageKey());
    }
}