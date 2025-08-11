package com.sysml.platform.infrastructure.emf;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * RQ-INFRA-EMF-003: EMFModelManager
 * 验收条件：CRUD操作正确、DTO映射无损
 */
public class EMFModelManagerTest {
    
    private EMFModelManager modelManager;
    
    @BeforeEach
    public void setUp() {
        modelManager = new EMFModelManager();
    }
    
    @Test
    public void shouldCreateModelElement() {
        // Given: 一个模型元素数据
        ModelElementDTO dto = new ModelElementDTO();
        dto.setName("TestElement");
        dto.setType("Requirement");
        
        // When: 创建模型元素
        String id = modelManager.create(dto);
        
        // Then: 应该返回ID且能查询到
        assertNotNull(id);
        ModelElementDTO retrieved = modelManager.findById(id);
        assertNotNull(retrieved);
        assertEquals("TestElement", retrieved.getName());
        assertEquals("Requirement", retrieved.getType());
    }
    
    @Test
    public void shouldUpdateModelElement() {
        // Given: 创建一个元素
        ModelElementDTO dto = new ModelElementDTO();
        dto.setName("OriginalName");
        String id = modelManager.create(dto);
        
        // When: 更新元素
        dto.setName("UpdatedName");
        boolean updated = modelManager.update(id, dto);
        
        // Then: 更新应该成功
        assertTrue(updated);
        ModelElementDTO retrieved = modelManager.findById(id);
        assertEquals("UpdatedName", retrieved.getName());
    }
    
    @Test
    public void shouldDeleteModelElement() {
        // Given: 创建一个元素
        ModelElementDTO dto = new ModelElementDTO();
        dto.setName("ToDelete");
        String id = modelManager.create(dto);
        
        // When: 删除元素
        boolean deleted = modelManager.delete(id);
        
        // Then: 删除应该成功且查询不到
        assertTrue(deleted);
        assertNull(modelManager.findById(id));
    }
}