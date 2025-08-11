package com.sysml.platform.infrastructure.emf;

import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目仓库
 * 管理SysML项目的持久化
 */
@Repository
public class ProjectRepository {
    
    private final Map<String, Map<String, Object>> projects = new ConcurrentHashMap<>();
    
    public void save(Map<String, Object> project) {
        String id = (String) project.get("id");
        if (id == null) {
            throw new IllegalArgumentException("Project must have an id");
        }
        projects.put(id, new HashMap<>(project));
    }
    
    public Map<String, Object> findById(String id) {
        Map<String, Object> project = projects.get(id);
        return project != null ? new HashMap<>(project) : null;
    }
    
    public List<Map<String, Object>> findAll() {
        return new ArrayList<>(projects.values());
    }
    
    public void delete(String id) {
        projects.remove(id);
    }
}