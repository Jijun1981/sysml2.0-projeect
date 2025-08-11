package com.sysml.platform.ui.sirius;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.sysml.platform.infrastructure.emf.ProjectRepository;
import com.sysml.platform.infrastructure.emf.ElementRepository;

import java.util.*;

/**
 * RQ-UI-VIEWS-002: 视图服务
 * 实现四种视图类型的业务逻辑
 */
@Service
public class ViewsService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ElementRepository elementRepository;
    
    /**
     * 获取树视图数据
     */
    public Map<String, Object> getTreeView(String projectId) {
        Map<String, Object> project = projectRepository.findById(projectId);
        
        Map<String, Object> root = new HashMap<>();
        root.put("id", projectId);
        root.put("label", project != null ? project.get("name") : "Project");
        root.put("expanded", true);
        root.put("children", getTreeChildren(projectId));
        
        Map<String, Object> treeData = new HashMap<>();
        treeData.put("viewType", "tree");
        treeData.put("root", root);
        
        return treeData;
    }
    
    private List<Map<String, Object>> getTreeChildren(String parentId) {
        List<Map<String, Object>> children = new ArrayList<>();
        List<Map<String, Object>> elements = elementRepository.findByProjectId(parentId);
        
        for (Map<String, Object> element : elements) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", element.get("id"));
            node.put("label", element.get("name"));
            node.put("type", element.get("type"));
            node.put("expanded", false);
            node.put("children", new ArrayList<>());
            children.add(node);
        }
        
        return children;
    }
    
    /**
     * 获取表视图数据
     */
    public Map<String, Object> getTableView(String projectId, String type) {
        List<Map<String, Object>> elements = elementRepository.findByProjectId(projectId);
        
        // 定义列
        List<Map<String, Object>> columns = Arrays.asList(
            Map.of("key", "name", "label", "名称", "sortable", true),
            Map.of("key", "type", "label", "类型", "sortable", true),
            Map.of("key", "id", "label", "ID", "sortable", false),
            Map.of("key", "created", "label", "创建时间", "sortable", true)
        );
        
        // 准备行数据
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> element : elements) {
            if ("all".equals(type) || type.equals(element.get("type"))) {
                rows.add(element);
            }
        }
        
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("viewType", "table");
        tableData.put("columns", columns);
        tableData.put("rows", rows);
        tableData.put("sortable", true);
        tableData.put("filterable", true);
        
        return tableData;
    }
    
    /**
     * 获取图视图数据
     */
    public Map<String, Object> getDiagramView(String projectId) {
        List<Map<String, Object>> elements = elementRepository.findByProjectId(projectId);
        List<Map<String, Object>> relations = elementRepository.findRelationsByProjectId(projectId);
        
        // 准备节点
        List<Map<String, Object>> nodes = new ArrayList<>();
        int x = 100, y = 100;
        for (Map<String, Object> element : elements) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", element.get("id"));
            node.put("label", element.get("name"));
            node.put("type", element.get("type"));
            node.put("position", Map.of("x", x, "y", y));
            node.put("size", Map.of("width", 150, "height", 80));
            nodes.add(node);
            
            x += 200;
            if (x > 500) {
                x = 100;
                y += 150;
            }
        }
        
        // 准备边
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map<String, Object> relation : relations) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", relation.get("id"));
            edge.put("source", relation.get("sourceId"));
            edge.put("target", relation.get("targetId"));
            edge.put("type", relation.get("type"));
            edges.add(edge);
        }
        
        Map<String, Object> diagramData = new HashMap<>();
        diagramData.put("viewType", "diagram");
        diagramData.put("nodes", nodes);
        diagramData.put("edges", edges);
        diagramData.put("zoomLevel", 1.0);
        diagramData.put("viewport", Map.of("x", 0, "y", 0, "width", 800, "height", 600));
        
        return diagramData;
    }
    
    /**
     * 获取表单视图数据
     */
    public Map<String, Object> getFormView(String elementId) {
        Map<String, Object> element = elementRepository.findById(elementId);
        
        // 定义字段
        List<Map<String, Object>> fields = new ArrayList<>();
        
        fields.add(Map.of(
            "name", "name",
            "type", "text",
            "label", "名称",
            "value", element.getOrDefault("name", ""),
            "editable", true,
            "required", true
        ));
        
        fields.add(Map.of(
            "name", "type",
            "type", "text",
            "label", "类型",
            "value", element.getOrDefault("type", ""),
            "editable", false,
            "required", true
        ));
        
        if ("Requirement".equals(element.get("type"))) {
            fields.add(Map.of(
                "name", "text",
                "type", "textarea",
                "label", "需求文本",
                "value", element.getOrDefault("text", ""),
                "editable", true,
                "required", false
            ));
            
            fields.add(Map.of(
                "name", "priority",
                "type", "select",
                "label", "优先级",
                "value", element.getOrDefault("priority", "MEDIUM"),
                "options", Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL"),
                "editable", true,
                "required", false
            ));
        }
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("viewType", "form");
        formData.put("elementId", elementId);
        formData.put("fields", fields);
        formData.put("validationRules", getValidationRules(element.get("type").toString()));
        formData.put("submitUrl", "/api/views/form/" + elementId);
        
        return formData;
    }
    
    private Map<String, Object> getValidationRules(String type) {
        Map<String, Object> rules = new HashMap<>();
        rules.put("name", Map.of("required", true, "maxLength", 100));
        
        if ("Requirement".equals(type)) {
            rules.put("text", Map.of("maxLength", 1000));
            rules.put("priority", Map.of("enum", Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL")));
        }
        
        return rules;
    }
    
    /**
     * 更新元素
     */
    public Map<String, Object> updateElement(String elementId, Map<String, Object> updates) {
        Map<String, Object> element = elementRepository.findById(elementId);
        if (element == null) {
            throw new IllegalArgumentException("Element not found: " + elementId);
        }
        
        // 更新允许的字段
        if (updates.containsKey("name")) {
            element.put("name", updates.get("name"));
        }
        if (updates.containsKey("text")) {
            element.put("text", updates.get("text"));
        }
        if (updates.containsKey("priority")) {
            element.put("priority", updates.get("priority"));
        }
        
        elementRepository.save(element);
        return element;
    }
}