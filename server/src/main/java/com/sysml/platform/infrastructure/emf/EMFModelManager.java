package com.sysml.platform.infrastructure.emf;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EMF模型管理器
 * 实现RQ-INFRA-EMF-003: EMFModelManager
 * 负责统一管理EObject生命周期和DTO映射
 */
@Component
public class EMFModelManager {
    
    // 暂时用内存存储，后续集成CDO
    private final Map<String, ModelElementDTO> repository = new ConcurrentHashMap<>();
    
    /**
     * 创建模型元素
     */
    public String create(ModelElementDTO dto) {
        String id = UUID.randomUUID().toString();
        dto.setId(id);
        repository.put(id, dto);
        return id;
    }
    
    /**
     * 根据ID查找元素
     */
    public ModelElementDTO findById(String id) {
        return repository.get(id);
    }
    
    /**
     * 更新模型元素
     */
    public boolean update(String id, ModelElementDTO dto) {
        if (!repository.containsKey(id)) {
            return false;
        }
        dto.setId(id);
        repository.put(id, dto);
        return true;
    }
    
    /**
     * 删除模型元素
     */
    public boolean delete(String id) {
        return repository.remove(id) != null;
    }
}