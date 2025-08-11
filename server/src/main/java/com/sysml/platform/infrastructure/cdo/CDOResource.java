package com.sysml.platform.infrastructure.cdo;

import java.util.HashMap;
import java.util.Map;

/**
 * CDO资源对象
 * 代表一个可持久化的资源
 */
public class CDOResource {
    
    private final String path;
    private final Map<String, Object> attributes = new HashMap<>();
    
    public CDOResource(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
}