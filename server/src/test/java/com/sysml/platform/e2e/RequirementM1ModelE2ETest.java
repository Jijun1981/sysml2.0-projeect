package com.sysml.platform.e2e;

import com.sysml.platform.domain.requirements.*;
import com.sysml.platform.infrastructure.emf.EMFRequirementAdapter;
import com.sysml.platform.m2.SysMLModelAdapter;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端需求M1模型测试
 * 测试从前端GraphQL到EMF持久化的完整流程
 * 
 * 测试场景：UAV无人机系统需求建模
 * - 创建系统级需求
 * - 创建子系统需求并建立derive关系
 * - 创建详细需求并建立refine关系
 * - 查询需求层次结构
 * - 验证EMF模型持久化
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("需求M1模型端到端测试")
public class RequirementM1ModelE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private RequirementService requirementService;
    
    @Autowired
    private EMFRequirementAdapter emfAdapter;
    
    @Autowired
    private SysMLModelAdapter modelAdapter;
    
    private HttpHeaders headers;
    
    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
    
    @Test
    @DisplayName("UAV系统需求建模完整场景")
    void testUAVRequirementModelingE2E() {
        
        // ========== 步骤1: 创建系统级需求 ==========
        System.out.println("\n=== 步骤1: 创建UAV系统级需求 ===");
        
        String createSystemReq = """
            mutation {
                createRequirement(input: {
                    reqId: "UAV-SYS-001"
                    name: "UAV系统总体需求"
                    text: "无人机系统应支持自主飞行、实时监控和数据采集功能"
                    kind: FUNCTIONAL
                    priority: CRITICAL
                }) {
                    ok
                    requirement {
                        id
                        name
                        text
                        kind
                        priority
                        status
                    }
                    error {
                        code
                        messageKey
                    }
                }
            }
        """;
        
        Map<String, Object> systemReqResponse = executeGraphQL(createSystemReq);
        assertNotNull(systemReqResponse);
        Map<String, Object> createResult = getDataField(systemReqResponse, "createRequirement");
        assertTrue((Boolean) createResult.get("ok"), "系统需求创建应该成功");
        
        Map<String, Object> systemReq = (Map<String, Object>) createResult.get("requirement");
        assertNotNull(systemReq);
        assertEquals("UAV-SYS-001", systemReq.get("id"));
        System.out.println("✓ 创建系统需求: " + systemReq.get("name"));
        
        // ========== 步骤2: 创建飞控子系统需求 ==========
        System.out.println("\n=== 步骤2: 创建飞控子系统需求 ===");
        
        String createFlightReq = """
            mutation {
                createRequirement(input: {
                    reqId: "UAV-FLT-001"
                    name: "飞行控制子系统需求"
                    text: "飞控系统应提供姿态控制、航线规划和自动返航功能"
                    kind: FUNCTIONAL
                    priority: HIGH
                }) {
                    ok
                    requirement { id name }
                }
            }
        """;
        
        Map<String, Object> flightReqResponse = executeGraphQL(createFlightReq);
        Map<String, Object> flightResult = getDataField(flightReqResponse, "createRequirement");
        assertTrue((Boolean) flightResult.get("ok"));
        System.out.println("✓ 创建飞控需求: UAV-FLT-001");
        
        // ========== 步骤3: 创建通信子系统需求 ==========
        System.out.println("\n=== 步骤3: 创建通信子系统需求 ===");
        
        String createCommReq = """
            mutation {
                createRequirement(input: {
                    reqId: "UAV-COM-001"
                    name: "通信子系统需求"
                    text: "通信系统应支持实时视频传输、遥测数据回传和远程控制"
                    kind: FUNCTIONAL
                    priority: HIGH
                }) {
                    ok
                    requirement { id name }
                }
            }
        """;
        
        Map<String, Object> commReqResponse = executeGraphQL(createCommReq);
        Map<String, Object> commResult = getDataField(commReqResponse, "createRequirement");
        assertTrue((Boolean) commResult.get("ok"));
        System.out.println("✓ 创建通信需求: UAV-COM-001");
        
        // ========== 步骤4: 建立derive关系 ==========
        System.out.println("\n=== 步骤4: 建立派生关系 ===");
        
        // 飞控需求派生自系统需求
        String deriveFlightFromSystem = """
            mutation {
                deriveRequirement(
                    sourceId: "UAV-FLT-001"
                    targetId: "UAV-SYS-001"
                ) {
                    ok
                    targetRequirement { id name }
                    error { code messageKey }
                }
            }
        """;
        
        Map<String, Object> derive1Response = executeGraphQL(deriveFlightFromSystem);
        Map<String, Object> derive1Result = getDataField(derive1Response, "deriveRequirement");
        assertTrue((Boolean) derive1Result.get("ok"), "飞控派生关系应该建立成功");
        System.out.println("✓ 建立关系: UAV-FLT-001 derives from UAV-SYS-001");
        
        // 通信需求派生自系统需求
        String deriveCommFromSystem = """
            mutation {
                deriveRequirement(
                    sourceId: "UAV-COM-001"
                    targetId: "UAV-SYS-001"
                ) {
                    ok
                }
            }
        """;
        
        Map<String, Object> derive2Response = executeGraphQL(deriveCommFromSystem);
        Map<String, Object> derive2Result = getDataField(derive2Response, "deriveRequirement");
        assertTrue((Boolean) derive2Result.get("ok"));
        System.out.println("✓ 建立关系: UAV-COM-001 derives from UAV-SYS-001");
        
        // ========== 步骤5: 创建详细需求并建立refine关系 ==========
        System.out.println("\n=== 步骤5: 创建详细需求 ===");
        
        // 创建姿态控制详细需求
        String createAttitudeReq = """
            mutation {
                createRequirement(input: {
                    reqId: "UAV-FLT-002"
                    name: "姿态控制精度需求"
                    text: "飞控系统应保持俯仰、横滚、偏航角度控制精度在±2度以内"
                    kind: PERFORMANCE
                    priority: HIGH
                }) {
                    ok
                    requirement { id name }
                }
            }
        """;
        
        Map<String, Object> attitudeResponse = executeGraphQL(createAttitudeReq);
        assertTrue((Boolean) getDataField(attitudeResponse, "createRequirement").get("ok"));
        System.out.println("✓ 创建详细需求: UAV-FLT-002 (姿态控制)");
        
        // 建立refine关系
        String refineAttitude = """
            mutation {
                refineRequirement(
                    sourceId: "UAV-FLT-002"
                    targetId: "UAV-FLT-001"
                ) {
                    ok
                    error { code messageKey }
                }
            }
        """;
        
        Map<String, Object> refineResponse = executeGraphQL(refineAttitude);
        Map<String, Object> refineResult = getDataField(refineResponse, "refineRequirement");
        assertTrue((Boolean) refineResult.get("ok"), "细化关系应该建立成功");
        System.out.println("✓ 建立关系: UAV-FLT-002 refines UAV-FLT-001");
        
        // ========== 步骤6: 查询需求层次结构 ==========
        System.out.println("\n=== 步骤6: 查询需求层次结构 ===");
        
        String queryHierarchy = """
            query {
                requirements(page: 0, size: 20) {
                    ok
                    content {
                        id
                        name
                        kind
                        priority
                        derivedFrom { id name }
                        derives { id name }
                        refinedFrom { id name }
                        refines { id name }
                    }
                    totalElements
                }
            }
        """;
        
        Map<String, Object> queryResponse = executeGraphQL(queryHierarchy);
        Map<String, Object> queryResult = getDataField(queryResponse, "requirements");
        assertTrue((Boolean) queryResult.get("ok"));
        
        Object contentObj = queryResult.get("content");
        assertNotNull(contentObj, "查询结果不应为空");
        System.out.println("✓ 查询到需求总数: " + queryResult.get("totalElements"));
        
        // ========== 步骤7: 验证EMF模型转换 ==========
        System.out.println("\n=== 步骤7: 验证EMF模型 ===");
        
        // 通过服务层获取需求
        QueryRequirementsPayload serviceResult = requirementService.queryRequirements(
            QueryRequirementsInput.builder()
                .keyword("UAV-SYS-001")
                .page(0)
                .size(1)
                .build()
        );
        
        assertFalse(serviceResult.getContent().isEmpty(), "应该能查询到系统需求");
        Object reqObj = serviceResult.getContent().get(0);
        Requirement sysReq = null;
        if (reqObj instanceof Requirement) {
            sysReq = (Requirement) reqObj;
        } else if (reqObj instanceof RequirementDefinition) {
            RequirementDefinition reqDef = (RequirementDefinition) reqObj;
            sysReq = new Requirement();
            sysReq.setId(reqDef.getId());
            sysReq.setName(reqDef.getName());
            sysReq.setText(reqDef.getText());
            sysReq.setKind(reqDef.getKind());
            sysReq.setPriority(reqDef.getPriority());
            sysReq.setStatus(reqDef.getStatus());
        }
        assertNotNull(sysReq, "需求对象不应为空");
        
        // 转换为EMF模型
        EObject emfModel = emfAdapter.toEMFModel(sysReq);
        assertNotNull(emfModel, "EMF模型转换不应为空");
        
        // 验证EMF属性
        assertEquals("UAV-SYS-001", emfModel.eGet(emfModel.eClass().getEStructuralFeature("id")));
        assertEquals("UAV系统总体需求", emfModel.eGet(emfModel.eClass().getEStructuralFeature("name")));
        System.out.println("✓ EMF模型转换成功");
        
        // 验证反向转换
        Requirement convertedBack = emfAdapter.fromEMFModel(emfModel);
        assertNotNull(convertedBack);
        assertEquals(sysReq.getId(), convertedBack.getId());
        assertEquals(sysReq.getName(), convertedBack.getName());
        System.out.println("✓ EMF模型反向转换成功");
        
        // ========== 步骤8: 测试循环检测 ==========
        System.out.println("\n=== 步骤8: 测试循环依赖检测 ===");
        
        // 尝试创建循环：UAV-SYS-001 -> UAV-FLT-001 (会形成循环)
        String createCycle = """
            mutation {
                deriveRequirement(
                    sourceId: "UAV-SYS-001"
                    targetId: "UAV-FLT-001"
                ) {
                    ok
                    error { 
                        code 
                        messageKey 
                    }
                }
            }
        """;
        
        Map<String, Object> cycleResponse = executeGraphQL(createCycle);
        Map<String, Object> cycleResult = getDataField(cycleResponse, "deriveRequirement");
        assertFalse((Boolean) cycleResult.get("ok"), "循环依赖应该被检测到");
        
        Map<String, Object> error = (Map<String, Object>) cycleResult.get("error");
        assertNotNull(error);
        assertEquals("REQ_CYCLE_DETECTED", error.get("code"));
        System.out.println("✓ 循环依赖检测正常工作");
        
        // ========== 步骤9: 验证持久化 ==========
        System.out.println("\n=== 步骤9: 验证持久化能力 ===");
        
        // 同步到EMF
        emfAdapter.syncWithEMF();
        
        // 验证一致性
        boolean isConsistent = emfAdapter.validateConsistency();
        assertTrue(isConsistent, "EMF模型应该与服务层保持一致");
        System.out.println("✓ EMF模型同步和一致性验证通过");
        
        // ========== 测试总结 ==========
        System.out.println("\n========================================");
        System.out.println("端到端测试完成！");
        System.out.println("✓ 创建了4个需求");
        System.out.println("✓ 建立了2个derive关系");
        System.out.println("✓ 建立了1个refine关系");
        System.out.println("✓ 验证了循环检测");
        System.out.println("✓ 验证了EMF模型转换");
        System.out.println("✓ 验证了持久化能力");
        System.out.println("========================================\n");
    }
    
    @Test
    @DisplayName("测试需求的完整CRUD操作")
    void testRequirementCRUDOperations() {
        System.out.println("\n=== 测试CRUD操作 ===");
        
        // Create
        String createMutation = """
            mutation {
                createRequirement(input: {
                    reqId: "TEST-CRUD-001"
                    name: "测试需求"
                    text: "这是一个测试需求"
                    kind: FUNCTIONAL
                    priority: MEDIUM
                }) {
                    ok
                    requirement {
                        id
                        name
                        status
                    }
                }
            }
        """;
        
        Map<String, Object> createResponse = executeGraphQL(createMutation);
        Map<String, Object> createResult = getDataField(createResponse, "createRequirement");
        assertTrue((Boolean) createResult.get("ok"));
        System.out.println("✓ Create: 创建需求成功");
        
        // Read
        String readQuery = """
            query {
                requirement(id: "TEST-CRUD-001") {
                    id
                    name
                    text
                }
            }
        """;
        
        Map<String, Object> readResponse = executeGraphQL(readQuery);
        assertNotNull(getDataField(readResponse, "requirement"));
        System.out.println("✓ Read: 读取需求成功");
        
        // Update
        String updateMutation = """
            mutation {
                updateRequirement(
                    id: "TEST-CRUD-001"
                    input: {
                        name: "更新后的测试需求"
                        priority: HIGH
                    }
                ) {
                    ok
                    requirement {
                        name
                        priority
                    }
                }
            }
        """;
        
        Map<String, Object> updateResponse = executeGraphQL(updateMutation);
        Map<String, Object> updateResult = getDataField(updateResponse, "updateRequirement");
        assertTrue((Boolean) updateResult.get("ok"));
        System.out.println("✓ Update: 更新需求成功");
        
        // Delete
        String deleteMutation = """
            mutation {
                deleteRequirement(id: "TEST-CRUD-001") {
                    ok
                }
            }
        """;
        
        Map<String, Object> deleteResponse = executeGraphQL(deleteMutation);
        Map<String, Object> deleteResult = getDataField(deleteResponse, "deleteRequirement");
        assertTrue((Boolean) deleteResult.get("ok"));
        System.out.println("✓ Delete: 删除需求成功");
    }
    
    /**
     * 执行GraphQL查询
     */
    private Map<String, Object> executeGraphQL(String query) {
        Map<String, Object> request = new HashMap<>();
        request.put("query", query);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "/graphql",
            HttpMethod.POST,
            entity,
            Map.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        // 检查是否有错误
        if (body.containsKey("errors")) {
            System.err.println("GraphQL错误: " + body.get("errors"));
        }
        
        return body;
    }
    
    /**
     * 从GraphQL响应中提取data字段
     */
    private Map<String, Object> getDataField(Map<String, Object> response, String field) {
        assertNotNull(response);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data, "响应中应该包含data字段");
        return (Map<String, Object>) data.get(field);
    }
}