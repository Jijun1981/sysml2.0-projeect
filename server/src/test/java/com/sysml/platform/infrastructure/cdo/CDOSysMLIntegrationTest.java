package com.sysml.platform.infrastructure.cdo;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 集成测试：验证SysML v2元模型通过CDO存储到PostgreSQL
 * 测试RequirementUsage、PartUsage和Connector的存储和加载
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_test_db",
    "spring.datasource.username=sysml_user",
    "spring.datasource.password=sysml_password",
    "cdo.enabled=true",
    "cdo.store.drop-on-activate=true"
})
class CDOSysMLIntegrationTest {

  @Autowired(required = false)
  private CDOServerManager cdoServerManager;

  @Autowired(required = false)
  private CDOModelService cdoModelService;
  
  private EPackage sysmlPackage;
  private EClass requirementUsageClass;
  private EClass partUsageClass;
  private EClass connectorClass;

  @BeforeEach
  void loadSysMLMetamodel() {
    // 注册Ecore资源工厂
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
    
    // 加载SysML.ecore元模型
    ResourceSet resourceSet = new ResourceSetImpl();
    URI ecoreURI = URI.createFileURI("src/main/resources/ecore/sysml/SysML.ecore");
    Resource ecoreResource = resourceSet.getResource(ecoreURI, true);
    
    assertThat(ecoreResource).isNotNull();
    assertThat(ecoreResource.getContents()).isNotEmpty();
    
    // 获取SysML包
    sysmlPackage = (EPackage) ecoreResource.getContents().get(0);
    assertThat(sysmlPackage.getName()).isEqualTo("sysml");
    assertThat(sysmlPackage.getNsURI()).isEqualTo("https://www.omg.org/spec/SysML/20250201");
    
    // 获取关键的EClass
    requirementUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
    partUsageClass = (EClass) sysmlPackage.getEClassifier("PartUsage");
    connectorClass = (EClass) sysmlPackage.getEClassifier("Connector");
    
    assertThat(requirementUsageClass).isNotNull();
    assertThat(partUsageClass).isNotNull();
    assertThat(connectorClass).isNotNull();
  }
  
  private void ensurePackageRegistered() {
    // 在每个测试中确保包已注册
    if (cdoModelService != null && sysmlPackage != null) {
      try {
        cdoModelService.registerPackage(sysmlPackage);
      } catch (Exception e) {
        // 包可能已经注册，忽略错误
        System.out.println("Package registration info: " + e.getMessage());
      }
    }
  }

  @Test
  void testCDOServerWithSysMLMetamodel() {
    assertThat(cdoServerManager).isNotNull();
    assertThat(cdoServerManager.isHealthy()).isTrue();
    assertThat(cdoServerManager.getRepository()).isNotNull();
    assertThat(cdoServerManager.getSession()).isNotNull();
  }

  @Test
  void testStoreAndLoadRequirementUsage() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    assertThat(requirementUsageClass).isNotNull();
    
    // 创建RequirementUsage实例
    EObject requirementUsage = sysmlPackage.getEFactoryInstance().create(requirementUsageClass);
    
    // 设置属性（如果有name属性）
    var nameAttribute = requirementUsageClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    if (nameAttribute.isPresent()) {
      requirementUsage.eSet(nameAttribute.get(), "REQ-001: System shall support CDO persistence");
    }
    
    // 使用CDO事务存储
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      Resource resource = transaction.createResource("/sysml/requirements");
      resource.getContents().add(requirementUsage);
      transaction.commit();
      
      assertThat(requirementUsage.eResource()).isNotNull();
      
    } finally {
      transaction.close();
    }
    
    // 从新事务读取验证
    CDOTransaction readTransaction = cdoServerManager.openTransaction();
    try {
      Resource readResource = readTransaction.getResource("/sysml/requirements");
      assertThat(readResource).isNotNull();
      assertThat(readResource.getContents()).hasSize(1);
      
      EObject loadedReq = readResource.getContents().get(0);
      assertThat(loadedReq.eClass()).isEqualTo(requirementUsageClass);
      
      if (nameAttribute.isPresent()) {
        assertThat(loadedReq.eGet(nameAttribute.get())).isEqualTo("REQ-001: System shall support CDO persistence");
      }
      
    } finally {
      readTransaction.close();
    }
  }

  @Test
  void testStoreAndLoadPartUsage() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    assertThat(partUsageClass).isNotNull();
    
    // 创建PartUsage实例
    EObject partUsage = sysmlPackage.getEFactoryInstance().create(partUsageClass);
    
    // 设置属性
    var nameAttribute = partUsageClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    if (nameAttribute.isPresent()) {
      partUsage.eSet(nameAttribute.get(), "Engine");
    }
    
    // 使用CDO事务存储
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      Resource resource = transaction.createResource("/sysml/parts");
      resource.getContents().add(partUsage);
      transaction.commit();
      
      assertThat(partUsage.eResource()).isNotNull();
      
    } finally {
      transaction.close();
    }
    
    // 验证持久化
    CDOTransaction readTransaction = cdoServerManager.openTransaction();
    try {
      Resource readResource = readTransaction.getResource("/sysml/parts");
      assertThat(readResource).isNotNull();
      assertThat(readResource.getContents()).hasSize(1);
      
      EObject loadedPart = readResource.getContents().get(0);
      assertThat(loadedPart.eClass()).isEqualTo(partUsageClass);
      
      if (nameAttribute.isPresent()) {
        assertThat(loadedPart.eGet(nameAttribute.get())).isEqualTo("Engine");
      }
      
    } finally {
      readTransaction.close();
    }
  }

  @Test
  void testStoreAndLoadConnector() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    assertThat(connectorClass).isNotNull();
    
    // 创建两个PartUsage作为连接的端点
    EObject part1 = sysmlPackage.getEFactoryInstance().create(partUsageClass);
    EObject part2 = sysmlPackage.getEFactoryInstance().create(partUsageClass);
    
    var nameAttribute = partUsageClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    if (nameAttribute.isPresent()) {
      part1.eSet(nameAttribute.get(), "FuelTank");
      part2.eSet(nameAttribute.get(), "Engine");
    }
    
    // 创建Connector
    EObject connector = sysmlPackage.getEFactoryInstance().create(connectorClass);
    
    var connectorNameAttr = connectorClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    if (connectorNameAttr.isPresent()) {
      connector.eSet(connectorNameAttr.get(), "FuelLine");
    }
    
    // 存储到CDO
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      Resource resource = transaction.createResource("/sysml/model");
      resource.getContents().add(part1);
      resource.getContents().add(part2);
      resource.getContents().add(connector);
      transaction.commit();
      
      assertThat(part1.eResource()).isNotNull();
      assertThat(part2.eResource()).isNotNull();
      assertThat(connector.eResource()).isNotNull();
      
    } finally {
      transaction.close();
    }
    
    // 验证完整模型持久化
    CDOTransaction readTransaction = cdoServerManager.openTransaction();
    try {
      Resource readResource = readTransaction.getResource("/sysml/model");
      assertThat(readResource).isNotNull();
      assertThat(readResource.getContents()).hasSize(3);
      
      // 验证各个元素类型
      assertThat(readResource.getContents().get(0).eClass()).isEqualTo(partUsageClass);
      assertThat(readResource.getContents().get(1).eClass()).isEqualTo(partUsageClass);
      assertThat(readResource.getContents().get(2).eClass()).isEqualTo(connectorClass);
      
    } finally {
      readTransaction.close();
    }
  }

  @Test
  void testCompleteEMFToCDOToPostgreSQLChain() throws CommitException {
    // 验证完整链路：EMF模型 → CDO存储 → PostgreSQL持久化
    assertThat(cdoServerManager).isNotNull();
    assertThat(cdoModelService).isNotNull();
    
    // 创建一个复杂的SysML模型结构
    EObject systemRequirement = sysmlPackage.getEFactoryInstance().create(requirementUsageClass);
    EObject systemPart = sysmlPackage.getEFactoryInstance().create(partUsageClass);
    
    var reqNameAttr = requirementUsageClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    var partNameAttr = partUsageClass.getEAllAttributes().stream()
        .filter(attr -> "name".equals(attr.getName()))
        .findFirst();
    
    if (reqNameAttr.isPresent()) {
      systemRequirement.eSet(reqNameAttr.get(), "System Performance Requirement");
    }
    
    if (partNameAttr.isPresent()) {
      systemPart.eSet(partNameAttr.get(), "Main System Component");
    }
    
    // 通过CDOModelService存储
    String reqResourceId = cdoModelService.saveModel(systemRequirement, "/sysml/system/requirements/perf");
    String partResourceId = cdoModelService.saveModel(systemPart, "/sysml/system/parts/main");
    
    assertThat(reqResourceId).isNotNull();
    assertThat(partResourceId).isNotNull();
    
    // 通过CDOModelService加载并验证
    EObject loadedReq = cdoModelService.loadModel("/sysml/system/requirements/perf");
    EObject loadedPart = cdoModelService.loadModel("/sysml/system/parts/main");
    
    assertThat(loadedReq).isNotNull();
    assertThat(loadedPart).isNotNull();
    assertThat(loadedReq.eClass()).isEqualTo(requirementUsageClass);
    assertThat(loadedPart.eClass()).isEqualTo(partUsageClass);
    
    // 验证属性值保持不变
    if (reqNameAttr.isPresent()) {
      assertThat(loadedReq.eGet(reqNameAttr.get())).isEqualTo("System Performance Requirement");
    }
    
    if (partNameAttr.isPresent()) {
      assertThat(loadedPart.eGet(partNameAttr.get())).isEqualTo("Main System Component");
    }
  }
}