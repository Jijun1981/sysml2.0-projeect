package com.sysml.platform.ui.sirius;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

/**
 * RQ-UI-VIEWS-002: 视图类型控制器
 * 提供四种视图类型：树/表/图/表单
 */
@RestController
@RequestMapping("/api/views")
public class ViewsController {
    
    @Autowired
    private ViewsService viewsService;
    
    @GetMapping("/types")
    public List<Map<String, Object>> getViewTypes() {
        return Arrays.asList(
            Map.of(
                "type", "tree",
                "label", "树视图",
                "icon", "tree",
                "description", "显示模型元素的层次结构"
            ),
            Map.of(
                "type", "table",
                "label", "表视图",
                "icon", "table",
                "description", "以表格形式显示元素列表"
            ),
            Map.of(
                "type", "diagram",
                "label", "图视图",
                "icon", "diagram",
                "description", "显示模型的图形化表示"
            ),
            Map.of(
                "type", "form",
                "label", "表单视图",
                "icon", "form",
                "description", "编辑元素的属性"
            )
        );
    }
    
    @GetMapping("/tree/{projectId}")
    public ResponseEntity<Map<String, Object>> getTreeView(@PathVariable String projectId) {
        Map<String, Object> treeData = viewsService.getTreeView(projectId);
        return ResponseEntity.ok(treeData);
    }
    
    @GetMapping("/table/{projectId}")
    public ResponseEntity<Map<String, Object>> getTableView(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "all") String type) {
        Map<String, Object> tableData = viewsService.getTableView(projectId, type);
        return ResponseEntity.ok(tableData);
    }
    
    @GetMapping("/diagram/{projectId}")
    public ResponseEntity<Map<String, Object>> getDiagramView(@PathVariable String projectId) {
        Map<String, Object> diagramData = viewsService.getDiagramView(projectId);
        return ResponseEntity.ok(diagramData);
    }
    
    @GetMapping("/form/{elementId}")
    public ResponseEntity<Map<String, Object>> getFormView(@PathVariable String elementId) {
        Map<String, Object> formData = viewsService.getFormView(elementId);
        return ResponseEntity.ok(formData);
    }
    
    @PutMapping("/form/{elementId}")
    public ResponseEntity<Map<String, Object>> updateFormData(
            @PathVariable String elementId,
            @RequestBody Map<String, Object> updates) {
        Map<String, Object> updated = viewsService.updateElement(elementId, updates);
        return ResponseEntity.ok(updated);
    }
}