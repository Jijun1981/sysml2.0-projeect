package com.sysml.platform.m2;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.eclipse.emf.ecore.EObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RQ-M2-ROUNDTRIP-003: 往返等价
 * 验收条件：XMI往返等价、JSON往返等价
 * 
 * TDD: 测试模型序列化和反序列化的等价性
 * 
 * @TestCase TC-M2-ROUNDTRIP-001
 * @TestCase TC-M2-ROUNDTRIP-002
 * @TestCase TC-M2-ROUNDTRIP-003
 * @TestCase TC-M2-ROUNDTRIP-004
 */
public class SysMLRoundTripTest {
    
    private SysMLSerializer serializer;
    private SysMLFactory factory;
    private Path tempDir;
    
    @BeforeEach
    public void setUp() throws Exception {
        serializer = new SysMLSerializer();
        factory = new SysMLFactory();
        
        // 初始化
        factory.initialize(
            "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/kerml.ecore",
            "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore"
        );
        serializer.initialize(factory.getRegistry());
        
        // 创建临时目录
        tempDir = Files.createTempDirectory("sysml-test");
    }
    
    /**
     * TC-M2-ROUNDTRIP-001: XMI序列化和反序列化
     */
    @Test
    @DisplayName("应该能将模型序列化为XMI并反序列化")
    public void shouldSerializeAndDeserializeToXMI() throws Exception {
        // Given: 创建一个需求模型
        EObject requirement = factory.createRequirement(
            "REQ-RT-001",
            "Roundtrip Test Requirement",
            "This requirement should survive serialization"
        );
        
        // When: 序列化到XMI
        File xmiFile = tempDir.resolve("test.xmi").toFile();
        boolean saved = serializer.saveToXMI(requirement, xmiFile);
        
        // Then: 保存成功
        assertTrue(saved);
        assertTrue(xmiFile.exists());
        
        // When: 反序列化
        EObject loaded = serializer.loadFromXMI(xmiFile);
        
        // Then: 对象应该等价
        assertNotNull(loaded);
        assertEquals(requirement.eClass().getName(), loaded.eClass().getName());
        
        // 验证属性保持不变
        Object originalId = factory.getAttributeValue(requirement, "reqId");
        Object loadedId = factory.getAttributeValue(loaded, "reqId");
        if (originalId == null) {
            originalId = factory.getAttributeValue(requirement, "elementId");
            loadedId = factory.getAttributeValue(loaded, "elementId");
        }
        assertEquals(originalId, loadedId);
    }
    
    /**
     * TC-M2-ROUNDTRIP-002: JSON序列化和反序列化
     */
    @Test
    @DisplayName("应该能将模型序列化为JSON并反序列化")
    public void shouldSerializeAndDeserializeToJSON() throws Exception {
        // Given: 创建一个部件模型
        EObject part = factory.createPart(
            "PART-RT-001",
            "Test Part",
            "component"
        );
        
        // When: 序列化到JSON
        String json = serializer.toJSON(part);
        
        // Then: JSON不为空
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("PartDefinition") || json.contains("PartUsage"));
        
        // When: 反序列化
        EObject loaded = serializer.fromJSON(json, part.eClass());
        
        // Then: 对象应该等价
        assertNotNull(loaded);
        assertEquals(part.eClass().getName(), loaded.eClass().getName());
        
        // 验证属性
        assertEquals(
            factory.getAttributeValue(part, "elementId"),
            factory.getAttributeValue(loaded, "elementId")
        );
    }
    
    /**
     * TC-M2-ROUNDTRIP-003: 复杂模型XMI往返
     */
    @Test
    @DisplayName("复杂模型应该保持XMI往返等价")
    public void shouldMaintainXMIRoundTripEquivalence() throws Exception {
        // Given: 创建包含多个元素的复杂模型
        EObject[] elements = factory.createBatch(new ElementSpec[] {
            new ElementSpec("RequirementDefinition", "REQ-COMPLEX-001", "Complex Req 1"),
            new ElementSpec("RequirementDefinition", "REQ-COMPLEX-002", "Complex Req 2"),
            new ElementSpec("PartDefinition", "PART-COMPLEX-001", "Complex Part")
        });
        
        // 创建一个包含所有元素的包
        EObject pkg = serializer.createPackage("TestPackage", elements);
        
        // When: XMI往返
        File xmiFile = tempDir.resolve("complex.xmi").toFile();
        serializer.saveToXMI(pkg, xmiFile);
        EObject loadedPkg = serializer.loadFromXMI(xmiFile);
        
        // Then: 包应该等价
        assertNotNull(loadedPkg);
        
        // 验证元素数量
        int originalCount = serializer.getPackageElements(pkg).size();
        int loadedCount = serializer.getPackageElements(loadedPkg).size();
        assertEquals(originalCount, loadedCount);
        
        // When: 再次保存
        File xmiFile2 = tempDir.resolve("complex2.xmi").toFile();
        serializer.saveToXMI(loadedPkg, xmiFile2);
        
        // Then: 两个文件应该语义等价（允许格式差异）
        String content1 = Files.readString(xmiFile.toPath());
        String content2 = Files.readString(xmiFile2.toPath());
        
        // 至少应该包含相同的元素ID
        assertTrue(content2.contains("REQ-COMPLEX-001"));
        assertTrue(content2.contains("REQ-COMPLEX-002"));
        assertTrue(content2.contains("PART-COMPLEX-001"));
    }
    
    /**
     * TC-M2-ROUNDTRIP-004: JSON往返保持语义
     */
    @Test
    @DisplayName("JSON往返应该保持语义等价")
    public void shouldMaintainJSONSemanticEquivalence() throws Exception {
        // Given: 创建需求
        EObject requirement = factory.createRequirement(
            "REQ-JSON-001",
            "JSON Test",
            "Test JSON serialization"
        );
        
        // When: 第一次JSON序列化
        String json1 = serializer.toJSON(requirement);
        
        // 反序列化
        EObject loaded = serializer.fromJSON(json1, requirement.eClass());
        
        // 再次序列化
        String json2 = serializer.toJSON(loaded);
        
        // Then: 两次JSON应该语义等价
        assertNotNull(json1);
        assertNotNull(json2);
        
        // 验证关键内容存在
        assertTrue(json2.contains("REQ-JSON-001"));
        
        // 验证结构相似（可能有顺序差异）
        assertTrue(json1.contains("elementId") == json2.contains("elementId"));
    }
    
    /**
     * TC-M2-ROUNDTRIP-005: 跨格式转换
     */
    @Test
    @DisplayName("应该支持XMI到JSON的转换")
    public void shouldConvertBetweenFormats() throws Exception {
        // Given: 创建并保存为XMI
        EObject part = factory.createPart("PART-CONVERT", "Convertible Part", "type");
        File xmiFile = tempDir.resolve("convert.xmi").toFile();
        serializer.saveToXMI(part, xmiFile);
        
        // When: 从XMI加载并转为JSON
        EObject fromXMI = serializer.loadFromXMI(xmiFile);
        String json = serializer.toJSON(fromXMI);
        
        // Then: JSON应该包含正确信息
        assertNotNull(json);
        assertTrue(json.contains("PART-CONVERT"));
        
        // When: 从JSON加载并保存为XMI
        EObject fromJSON = serializer.fromJSON(json, fromXMI.eClass());
        File xmiFile2 = tempDir.resolve("convert2.xmi").toFile();
        serializer.saveToXMI(fromJSON, xmiFile2);
        
        // Then: 新XMI文件应该存在且包含正确内容
        assertTrue(xmiFile2.exists());
        String xmiContent = Files.readString(xmiFile2.toPath());
        assertTrue(xmiContent.contains("PART-CONVERT"));
    }
}

/**
 * 元素规格（复用）
 */
class ElementSpec {
    private String type;
    private String id;
    private String name;
    
    public ElementSpec(String type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }
    
    public String getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}