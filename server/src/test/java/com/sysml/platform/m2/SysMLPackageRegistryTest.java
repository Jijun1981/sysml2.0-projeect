package com.sysml.platform.m2;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EAttribute;

/**
 * RQ-M2-REG-001: EPackage注册
 * 验收条件：Registry包含KerML/SysML
 * 
 * TDD: 先定义期望的包结构和元素
 * 
 * @TestCase TC-M2-REG-001
 * @TestCase TC-M2-REG-002
 * @TestCase TC-M2-REG-003
 */
public class SysMLPackageRegistryTest {
    
    private SysMLPackageRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new SysMLPackageRegistry();
    }
    
    /**
     * TC-M2-REG-001: 加载并注册SysML包
     */
    @Test
    @DisplayName("应该成功加载并注册SysML EPackage")
    public void shouldLoadAndRegisterSysMLPackage() {
        // Given: SysML.ecore文件路径
        String ecorePath = "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore";
        
        // When: 加载并注册包
        boolean loaded = registry.loadAndRegister(ecorePath);
        
        // Then: 包应该成功加载
        assertTrue(loaded);
        
        // 验证包已注册
        EPackage sysmlPackage = registry.getSysMLPackage();
        assertNotNull(sysmlPackage);
        assertEquals("sysml", sysmlPackage.getName());
        assertNotNull(sysmlPackage.getNsURI());
    }
    
    /**
     * TC-M2-REG-002: 获取核心元类
     */
    @Test
    @DisplayName("应该能获取SysML核心元类")
    public void shouldGetCoreMetaclasses() {
        // Given: 已加载的SysML包
        registry.loadAndRegister("/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore");
        
        // When: 获取核心元类
        EClass requirementClass = registry.getEClass("RequirementDefinition");
        EClass partClass = registry.getEClass("PartDefinition");
        EClass portClass = registry.getEClass("PortDefinition");
        EClass connectionClass = registry.getEClass("ConnectionDefinition");
        
        // Then: 所有核心元类都应该存在
        assertNotNull(requirementClass, "RequirementDefinition应该存在");
        assertEquals("RequirementDefinition", requirementClass.getName());
        
        assertNotNull(partClass, "PartDefinition应该存在");
        assertEquals("PartDefinition", partClass.getName());
        
        assertNotNull(portClass, "PortDefinition应该存在");
        assertEquals("PortDefinition", portClass.getName());
        
        assertNotNull(connectionClass, "ConnectionDefinition应该存在");
        assertEquals("ConnectionDefinition", connectionClass.getName());
    }
    
    /**
     * TC-M2-REG-003: 获取元素属性
     */
    @Test
    @DisplayName("应该能访问元类的属性")
    public void shouldAccessMetaclassAttributes() {
        // Given: 已加载的SysML包
        registry.loadAndRegister("/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore");
        
        // When: 获取RequirementDefinition的属性
        EClass requirementClass = registry.getEClass("RequirementDefinition");
        
        // Then: 应该能访问标准属性
        // 检查是否有name属性（继承自Element）
        EAttribute nameAttr = registry.getAttribute(requirementClass, "name");
        assertNotNull(nameAttr, "应该有name属性");
        
        // 检查是否有其他需求相关属性
        EAttribute textAttr = registry.getAttribute(requirementClass, "text");
        if (textAttr == null) {
            // 可能在文档属性中
            EAttribute docAttr = registry.getAttribute(requirementClass, "documentation");
            assertNotNull(docAttr, "应该有text或documentation属性");
        }
    }
    
    /**
     * TC-M2-REG-004: 加载KerML包
     */
    @Test
    @DisplayName("应该同时加载KerML包")
    public void shouldLoadKerMLPackage() {
        // Given: KerML.ecore文件路径
        String kernelPath = "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/kerml.ecore";
        
        // When: 加载KerML包
        boolean loaded = registry.loadKerMLPackage(kernelPath);
        
        // Then: KerML包应该加载成功
        assertTrue(loaded);
        
        EPackage kermlPackage = registry.getKerMLPackage();
        assertNotNull(kermlPackage);
        
        // KerML定义了基础元类
        EClass elementClass = registry.getKerMLClass("Element");
        assertNotNull(elementClass, "KerML应该定义Element基类");
    }
    
    /**
     * TC-M2-REG-005: 包依赖关系
     */
    @Test
    @DisplayName("SysML包应该依赖KerML包")
    public void shouldHavePackageDependencies() {
        // Given: 加载两个包
        registry.loadKerMLPackage("/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/kerml.ecore");
        registry.loadAndRegister("/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore");
        
        // When: 检查SysML的RequirementDefinition
        EClass requirementClass = registry.getEClass("RequirementDefinition");
        
        // Then: 应该继承自KerML的某个基类
        assertFalse(requirementClass.getESuperTypes().isEmpty(), 
            "RequirementDefinition应该有父类");
        
        // 验证继承链最终到达Element
        assertTrue(registry.isKindOf(requirementClass, "Element"),
            "RequirementDefinition应该是Element的子类");
    }
}