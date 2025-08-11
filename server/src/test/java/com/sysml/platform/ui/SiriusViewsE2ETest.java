package com.sysml.platform.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

/**
 * RQ-UI-VIEWS-002: 视图类型
 * 验收条件：四种视图可用（树/表/图/表单）
 * E2E测试验证所有视图类型端到端工作
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SiriusViewsE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E: 树视图应该可用并能展示模型层次结构")
    public void treeViewShouldBeAvailableE2E() {
        // Given: 创建一个测试项目
        Map<String, Object> project = Map.of(
            "name", "TestProject",
            "type", "SysML"
        );
        ResponseEntity<Map> projectResponse = restTemplate.postForEntity(
            "/api/projects", project, Map.class);
        assertEquals(HttpStatus.CREATED, projectResponse.getStatusCode());
        String projectId = (String) projectResponse.getBody().get("id");

        // When: 请求树视图
        ResponseEntity<Map> treeResponse = restTemplate.getForEntity(
            "/api/views/tree/" + projectId, Map.class);

        // Then: 树视图应该返回层次结构
        assertEquals(HttpStatus.OK, treeResponse.getStatusCode());
        Map<String, Object> treeData = treeResponse.getBody();
        assertNotNull(treeData);
        assertEquals("tree", treeData.get("viewType"));
        assertNotNull(treeData.get("root"), "应该有根节点");
        
        // 验证树结构
        Map<String, Object> root = (Map<String, Object>) treeData.get("root");
        assertEquals(projectId, root.get("id"));
        assertEquals("TestProject", root.get("label"));
        assertNotNull(root.get("children"), "应该有子节点数组");
        assertTrue(root.containsKey("expanded"), "应该有展开状态");
    }

    @Test
    @DisplayName("E2E: 表视图应该可用并能展示元素列表")
    public void tableViewShouldBeAvailableE2E() {
        // Given: 创建测试项目和元素
        Map<String, Object> project = Map.of(
            "name", "TableTestProject",
            "type", "SysML"
        );
        ResponseEntity<Map> projectResponse = restTemplate.postForEntity(
            "/api/projects", project, Map.class);
        String projectId = (String) projectResponse.getBody().get("id");

        // 添加一些元素
        Map<String, Object> element1 = Map.of(
            "name", "Requirement1",
            "type", "Requirement",
            "projectId", projectId
        );
        restTemplate.postForEntity("/api/elements", element1, Map.class);

        Map<String, Object> element2 = Map.of(
            "name", "Block1",
            "type", "Block",
            "projectId", projectId
        );
        restTemplate.postForEntity("/api/elements", element2, Map.class);

        // When: 请求表视图
        ResponseEntity<Map> tableResponse = restTemplate.getForEntity(
            "/api/views/table/" + projectId + "?type=all", Map.class);

        // Then: 表视图应该返回结构化数据
        assertEquals(HttpStatus.OK, tableResponse.getStatusCode());
        Map<String, Object> tableData = tableResponse.getBody();
        assertNotNull(tableData);
        assertEquals("table", tableData.get("viewType"));
        
        // 验证表结构
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tableData.get("columns");
        assertNotNull(columns, "应该有列定义");
        assertTrue(columns.size() >= 3, "至少应该有名称、类型、ID列");
        
        List<Map<String, Object>> rows = (List<Map<String, Object>>) tableData.get("rows");
        assertNotNull(rows, "应该有行数据");
        assertEquals(2, rows.size(), "应该有两个元素");
        
        // 验证排序和过滤功能
        assertNotNull(tableData.get("sortable"), "应该支持排序");
        assertNotNull(tableData.get("filterable"), "应该支持过滤");
    }

    @Test
    @DisplayName("E2E: 图视图应该可用并能展示模型关系")
    public void diagramViewShouldBeAvailableE2E() {
        // Given: 创建包含关系的模型
        Map<String, Object> project = Map.of(
            "name", "DiagramTestProject",
            "type", "SysML"
        );
        ResponseEntity<Map> projectResponse = restTemplate.postForEntity(
            "/api/projects", project, Map.class);
        String projectId = (String) projectResponse.getBody().get("id");

        // 创建块和关系
        Map<String, Object> block1 = Map.of(
            "name", "SystemBlock",
            "type", "Block",
            "projectId", projectId
        );
        ResponseEntity<Map> block1Response = restTemplate.postForEntity(
            "/api/elements", block1, Map.class);
        String block1Id = (String) block1Response.getBody().get("id");

        Map<String, Object> block2 = Map.of(
            "name", "SubsystemBlock",
            "type", "Block",
            "projectId", projectId
        );
        ResponseEntity<Map> block2Response = restTemplate.postForEntity(
            "/api/elements", block2, Map.class);
        String block2Id = (String) block2Response.getBody().get("id");

        // 创建关系
        Map<String, Object> relation = Map.of(
            "type", "Composition",
            "sourceId", block1Id,
            "targetId", block2Id
        );
        restTemplate.postForEntity("/api/relations", relation, Map.class);

        // When: 请求图视图
        ResponseEntity<Map> diagramResponse = restTemplate.getForEntity(
            "/api/views/diagram/" + projectId, Map.class);

        // Then: 图视图应该返回节点和边
        assertEquals(HttpStatus.OK, diagramResponse.getStatusCode());
        Map<String, Object> diagramData = diagramResponse.getBody();
        assertNotNull(diagramData);
        assertEquals("diagram", diagramData.get("viewType"));
        
        // 验证图结构
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) diagramData.get("nodes");
        assertNotNull(nodes, "应该有节点");
        assertEquals(2, nodes.size(), "应该有两个节点");
        
        List<Map<String, Object>> edges = (List<Map<String, Object>>) diagramData.get("edges");
        assertNotNull(edges, "应该有边");
        assertEquals(1, edges.size(), "应该有一条边");
        
        // 验证布局信息
        for (Map<String, Object> node : nodes) {
            assertNotNull(node.get("position"), "节点应该有位置信息");
            assertNotNull(node.get("size"), "节点应该有大小信息");
        }
        
        // 验证交互功能
        assertNotNull(diagramData.get("zoomLevel"), "应该有缩放级别");
        assertNotNull(diagramData.get("viewport"), "应该有视口信息");
    }

    @Test
    @DisplayName("E2E: 表单视图应该可用并能编辑元素属性")
    public void formViewShouldBeAvailableE2E() {
        // Given: 创建一个可编辑的元素
        Map<String, Object> project = Map.of(
            "name", "FormTestProject",
            "type", "SysML"
        );
        ResponseEntity<Map> projectResponse = restTemplate.postForEntity(
            "/api/projects", project, Map.class);
        String projectId = (String) projectResponse.getBody().get("id");

        Map<String, Object> requirement = Map.of(
            "name", "TestRequirement",
            "type", "Requirement",
            "projectId", projectId,
            "text", "The system shall...",
            "priority", "HIGH"
        );
        ResponseEntity<Map> reqResponse = restTemplate.postForEntity(
            "/api/elements", requirement, Map.class);
        String reqId = (String) reqResponse.getBody().get("id");

        // When: 请求表单视图
        ResponseEntity<Map> formResponse = restTemplate.getForEntity(
            "/api/views/form/" + reqId, Map.class);

        // Then: 表单视图应该返回可编辑的字段
        assertEquals(HttpStatus.OK, formResponse.getStatusCode());
        Map<String, Object> formData = formResponse.getBody();
        assertNotNull(formData);
        assertEquals("form", formData.get("viewType"));
        
        // 验证表单结构
        List<Map<String, Object>> fields = (List<Map<String, Object>>) formData.get("fields");
        assertNotNull(fields, "应该有字段定义");
        assertTrue(fields.size() >= 4, "至少应该有名称、类型、文本、优先级字段");
        
        // 验证字段属性
        for (Map<String, Object> field : fields) {
            assertNotNull(field.get("name"), "字段应该有名称");
            assertNotNull(field.get("type"), "字段应该有类型");
            assertNotNull(field.get("value"), "字段应该有值");
            assertNotNull(field.get("editable"), "字段应该标明是否可编辑");
            
            if ("priority".equals(field.get("name"))) {
                assertNotNull(field.get("options"), "优先级字段应该有选项");
            }
        }
        
        // When: 提交表单更新
        Map<String, Object> updates = Map.of(
            "name", "UpdatedRequirement",
            "text", "The system must...",
            "priority", "CRITICAL"
        );
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
            "/api/views/form/" + reqId,
            HttpMethod.PUT,
            new org.springframework.http.HttpEntity<>(updates),
            Map.class
        );

        // Then: 更新应该成功
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        Map<String, Object> updatedData = updateResponse.getBody();
        assertEquals("UpdatedRequirement", updatedData.get("name"));
        assertEquals("CRITICAL", updatedData.get("priority"));
        
        // 验证验证规则
        assertNotNull(formData.get("validationRules"), "应该有验证规则");
        assertNotNull(formData.get("submitUrl"), "应该有提交URL");
    }

    @Test
    @DisplayName("E2E: 所有四种视图应该在视图列表中可用")
    public void allFourViewsShouldBeAvailable() {
        // When: 获取可用视图类型列表
        ResponseEntity<List> response = restTemplate.exchange(
            "/api/views/types",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List>() {}
        );

        // Then: 应该包含所有四种视图类型
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> viewTypes = response.getBody();
        assertNotNull(viewTypes);
        assertEquals(4, viewTypes.size(), "应该有四种视图类型");
        
        // 验证每种视图类型
        boolean hasTree = false, hasTable = false, hasDiagram = false, hasForm = false;
        
        for (Map<String, Object> viewType : viewTypes) {
            String type = (String) viewType.get("type");
            assertNotNull(viewType.get("label"), "视图类型应该有标签");
            assertNotNull(viewType.get("icon"), "视图类型应该有图标");
            assertNotNull(viewType.get("description"), "视图类型应该有描述");
            
            switch (type) {
                case "tree":
                    hasTree = true;
                    assertEquals("树视图", viewType.get("label"));
                    break;
                case "table":
                    hasTable = true;
                    assertEquals("表视图", viewType.get("label"));
                    break;
                case "diagram":
                    hasDiagram = true;
                    assertEquals("图视图", viewType.get("label"));
                    break;
                case "form":
                    hasForm = true;
                    assertEquals("表单视图", viewType.get("label"));
                    break;
            }
        }
        
        assertTrue(hasTree, "应该有树视图");
        assertTrue(hasTable, "应该有表视图");
        assertTrue(hasDiagram, "应该有图视图");
        assertTrue(hasForm, "应该有表单视图");
    }
}