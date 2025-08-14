package com.sysml.platform.infrastructure.cdo;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 基础CDO集成测试 - 验证CDO核心功能
 * 不使用复杂的SysML模型，只使用简单的Ecore模型
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
class CDOBasicIntegrationTest {

  @Autowired(required = false)
  private CDOServerManager cdoServerManager;

  @Autowired(required = false)
  private CDOModelService cdoModelService;

  @Test
  void testCDOServerBasicFunctionality() {
    // 验证CDO服务器基本功能
    assertThat(cdoServerManager).isNotNull();
    assertThat(cdoServerManager.isHealthy()).isTrue();
    assertThat(cdoServerManager.getRepository()).isNotNull();
    assertThat(cdoServerManager.getSession()).isNotNull();
    
    // 验证服务器信息
    var serverInfo = cdoServerManager.getServerInfo();
    assertThat(serverInfo).isNotNull();
    assertThat(serverInfo.get("initialized")).isEqualTo(true);
    assertThat(serverInfo.get("repositoryName")).isEqualTo("sysml-test");
    assertThat(serverInfo.get("repositoryState")).isNotNull();
  }

  @Test
  void testStoreAndLoadSimpleEObject() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    
    // 创建一个简单的动态EMF模型
    EPackage testPackage = EcoreFactory.eINSTANCE.createEPackage();
    testPackage.setName("TestPackage");
    testPackage.setNsPrefix("test");
    testPackage.setNsURI("http://test/1.0");
    
    // 创建一个简单的EClass
    EClass testClass = EcoreFactory.eINSTANCE.createEClass();
    testClass.setName("TestEntity");
    
    // 添加一个name属性
    var nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
    nameAttribute.setName("name");
    nameAttribute.setEType(EcorePackage.Literals.ESTRING);
    testClass.getEStructuralFeatures().add(nameAttribute);
    
    // 添加一个value属性
    var valueAttribute = EcoreFactory.eINSTANCE.createEAttribute();
    valueAttribute.setName("value");
    valueAttribute.setEType(EcorePackage.Literals.EINT);
    testClass.getEStructuralFeatures().add(valueAttribute);
    
    // 将EClass添加到包中
    testPackage.getEClassifiers().add(testClass);
    
    // 创建测试实例
    EObject testObject = testPackage.getEFactoryInstance().create(testClass);
    testObject.eSet(nameAttribute, "Test Object 1");
    testObject.eSet(valueAttribute, 42);
    
    // 存储到CDO
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      // 注册包到事务的session
      transaction.getSession().getPackageRegistry().putEPackage(testPackage);
      
      CDOResource resource = transaction.createResource("/test/basic");
      resource.getContents().add(testObject);
      transaction.commit();
      
      assertThat(testObject.eResource()).isNotNull();
      System.out.println("Successfully stored object with CDO ID: " + resource.cdoID());
      
    } finally {
      transaction.close();
    }
    
    // 从新事务读取验证
    CDOTransaction readTransaction = cdoServerManager.openTransaction();
    try {
      // 重新注册包（新事务需要）
      readTransaction.getSession().getPackageRegistry().putEPackage(testPackage);
      
      CDOResource readResource = readTransaction.getResource("/test/basic");
      assertThat(readResource).isNotNull();
      assertThat(readResource.getContents()).hasSize(1);
      
      EObject loadedObject = readResource.getContents().get(0);
      assertThat(loadedObject.eGet(nameAttribute)).isEqualTo("Test Object 1");
      assertThat(loadedObject.eGet(valueAttribute)).isEqualTo(42);
      
      System.out.println("Successfully loaded object: " + loadedObject.eGet(nameAttribute));
      
    } finally {
      readTransaction.close();
    }
  }

  @Test
  void testCDOTransactionOperations() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    
    // 创建多个事务验证并发访问
    CDOTransaction tx1 = cdoServerManager.openTransaction();
    CDOTransaction tx2 = cdoServerManager.openTransaction();
    
    try {
      assertThat(tx1).isNotNull();
      assertThat(tx2).isNotNull();
      assertThat(tx1.isClosed()).isFalse();
      assertThat(tx2.isClosed()).isFalse();
      
      // 在tx1中创建资源
      CDOResource resource1 = tx1.createResource("/test/tx1");
      tx1.commit();
      
      // 在tx2中应该能看到tx1创建的资源
      CDOResource resource2 = tx2.getResource("/test/tx1");
      assertThat(resource2).isNotNull();
      
    } finally {
      if (!tx1.isClosed()) tx1.close();
      if (!tx2.isClosed()) tx2.close();
    }
  }

  @Test
  void testCDOModelServiceOperations() throws CommitException {
    assertThat(cdoModelService).isNotNull();
    
    // 创建一个简单的包和对象
    EPackage servicePackage = EcoreFactory.eINSTANCE.createEPackage();
    servicePackage.setName("ServicePackage");
    servicePackage.setNsPrefix("srv");
    servicePackage.setNsURI("http://service/1.0");
    
    EClass entityClass = EcoreFactory.eINSTANCE.createEClass();
    entityClass.setName("Entity");
    servicePackage.getEClassifiers().add(entityClass);
    
    // 创建实例
    EObject entity = servicePackage.getEFactoryInstance().create(entityClass);
    
    // 通过CDOModelService保存
    String resourceId = cdoModelService.saveModel(entity, "/service/entities");
    assertThat(resourceId).isNotNull();
    System.out.println("Saved entity with resource ID: " + resourceId);
    
    // 通过CDOModelService加载
    EObject loadedEntity = cdoModelService.loadModel("/service/entities");
    assertThat(loadedEntity).isNotNull();
    assertThat(loadedEntity.eClass().getName()).isEqualTo("Entity");
    
    // 验证CDO连接
    assertThat(cdoModelService.verifyCDOConnection()).isTrue();
  }

  @Test
  void testCDOResourceManagement() throws CommitException {
    assertThat(cdoServerManager).isNotNull();
    
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      // 创建多个资源
      CDOResource resource1 = transaction.createResource("/resources/r1");
      CDOResource resource2 = transaction.createResource("/resources/r2");
      CDOResource resource3 = transaction.createResource("/resources/r3");
      
      // 创建简单对象并添加到资源
      EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
      pkg.setName("ResourceTestPackage");
      pkg.setNsPrefix("rtp");
      pkg.setNsURI("http://resourcetest/1.0");
      
      EClass cls = EcoreFactory.eINSTANCE.createEClass();
      cls.setName("Item");
      pkg.getEClassifiers().add(cls);
      
      transaction.getSession().getPackageRegistry().putEPackage(pkg);
      
      EObject item1 = pkg.getEFactoryInstance().create(cls);
      EObject item2 = pkg.getEFactoryInstance().create(cls);
      EObject item3 = pkg.getEFactoryInstance().create(cls);
      
      resource1.getContents().add(item1);
      resource2.getContents().add(item2);
      resource3.getContents().add(item3);
      
      transaction.commit();
      
      // 验证资源已创建
      assertThat(resource1.cdoID()).isNotNull();
      assertThat(resource2.cdoID()).isNotNull();
      assertThat(resource3.cdoID()).isNotNull();
      
      System.out.println("Created 3 CDO resources successfully");
      
    } finally {
      transaction.close();
    }
    
    // 在新事务中验证资源存在
    CDOTransaction verifyTx = cdoServerManager.openTransaction();
    try {
      assertThat(verifyTx.hasResource("/resources/r1")).isTrue();
      assertThat(verifyTx.hasResource("/resources/r2")).isTrue();
      assertThat(verifyTx.hasResource("/resources/r3")).isTrue();
      
      System.out.println("All resources verified in new transaction");
      
    } finally {
      verifyTx.close();
    }
  }
}