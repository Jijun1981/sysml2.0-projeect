package com.sysml.platform.domain.requirements;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

/**
 * RQ-REQ-RELATION-005: 需求间关系
 * 验收条件：仅允许Req→Req关系
 * 
 * @TestCase TC-REQ-RELATION-001
 * @TestCase TC-REQ-RELATION-002
 */
public class RequirementRelationTest {
    
    private RequirementService service;
    
    @BeforeEach
    public void setUp() {
        service = new RequirementService();
    }
    
    /**
     * TC-REQ-RELATION-001: 创建refine关系
     */
    @Test
    @DisplayName("应该支持创建refine关系")
    public void shouldCreateRefineRelationship() {
        // Given: 创建两个需求
        CreateRequirementInput abstract_ = CreateRequirementInput.builder()
            .reqId("REQ-ABSTRACT-001")
            .name("High Level Requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput refined = CreateRequirementInput.builder()
            .reqId("REQ-REFINED-001")
            .name("Refined Requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        
        CreateRequirementPayload abstractResult = service.createRequirement(abstract_);
        CreateRequirementPayload refinedResult = service.createRequirement(refined);
        
        // When: 建立refine关系
        RefineRequirementPayload refineResult = service.refineRequirement(
            abstractResult.getRequirement().getId(),
            refinedResult.getRequirement().getId()
        );
        
        // Then: 关系应该建立成功
        assertTrue(refineResult.isOk());
        assertNull(refineResult.getError());
        assertEquals("REQ-ABSTRACT-001", refineResult.getSource().getReqId());
        assertEquals("REQ-REFINED-001", refineResult.getTarget().getReqId());
    }
    
    /**
     * TC-REQ-RELATION-002: 获取需求关系
     */
    @Test
    @DisplayName("应该能够查询需求的所有关系")
    public void shouldGetRequirementRelations() {
        // Given: 创建需求网络
        CreateRequirementInput parent = CreateRequirementInput.builder()
            .reqId("REQ-PARENT")
            .name("Parent Requirement")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput child1 = CreateRequirementInput.builder()
            .reqId("REQ-CHILD-1")
            .name("Child 1")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        CreateRequirementInput child2 = CreateRequirementInput.builder()
            .reqId("REQ-CHILD-2")
            .name("Child 2")
            .kind(RequirementKind.FUNCTIONAL)
            .build();
        
        CreateRequirementPayload parentResult = service.createRequirement(parent);
        CreateRequirementPayload child1Result = service.createRequirement(child1);
        CreateRequirementPayload child2Result = service.createRequirement(child2);
        
        // 建立关系
        service.deriveRequirement(
            parentResult.getRequirement().getId(),
            child1Result.getRequirement().getId()
        );
        service.deriveRequirement(
            parentResult.getRequirement().getId(),
            child2Result.getRequirement().getId()
        );
        
        // When: 查询关系
        RequirementRelationsPayload relations = service.getRequirementRelations(
            parentResult.getRequirement().getId()
        );
        
        // Then: 应该返回正确的关系
        assertTrue(relations.isOk());
        assertNotNull(relations.getDerives());
        assertEquals(2, relations.getDerives().size());
        assertTrue(relations.getDerives().contains(child1Result.getRequirement().getId()));
        assertTrue(relations.getDerives().contains(child2Result.getRequirement().getId()));
    }
}