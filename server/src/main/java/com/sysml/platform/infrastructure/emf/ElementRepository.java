package com.sysml.platform.infrastructure.emf;

import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 元素仓库
 * 管理SysML元素的持久化
 */
@Repository
public class ElementRepository {
    
    private final Map<String, Map<String, Object>> elements = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> relations = Collections.synchronizedList(new ArrayList<>());
    
    public void save(Map<String, Object> element) {
        String id = (String) element.get("id");
        if (id == null) {
            throw new IllegalArgumentException("Element must have an id");
        }
        elements.put(id, new HashMap<>(element));
    }
    
    public Map<String, Object> findById(String id) {
        Map<String, Object> element = elements.get(id);
        return element != null ? new HashMap<>(element) : null;
    }
    
    public List<Map<String, Object>> findByProjectId(String projectId) {
        return elements.values().stream()
            .filter(e -> projectId.equals(e.get("projectId")))
            .map(HashMap::new)
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> findAll() {
        return new ArrayList<>(elements.values());
    }
    
    public void delete(String id) {
        elements.remove(id);
    }
    
    public void saveRelation(Map<String, Object> relation) {
        relations.add(new HashMap<>(relation));
    }
    
    public List<Map<String, Object>> findRelationsByProjectId(String projectId) {
        // 找出项目中所有元素的ID
        Set<String> elementIds = elements.values().stream()
            .filter(e -> projectId.equals(e.get("projectId")))
            .map(e -> (String) e.get("id"))
            .collect(Collectors.toSet());
        
        // 返回涉及这些元素的关系
        return relations.stream()
            .filter(r -> elementIds.contains(r.get("sourceId")) || 
                        elementIds.contains(r.get("targetId")))
            .map(HashMap::new)
            .collect(Collectors.toList());
    }
}