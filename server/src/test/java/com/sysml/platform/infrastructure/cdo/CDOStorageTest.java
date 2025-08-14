package com.sysml.platform.infrastructure.cdo;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.util.UUID;

/**
 * CDO存储功能测试 - 测试EMF模型的存储和加载
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_test_db",
    "spring.datasource.username=sysml_user",
    "spring.datasource.password=sysml_password",
    "cdo.enabled=true",
    "cdo.repository.name=sysml-test",
    "cdo.server.port=2037",
    "cdo.store.drop-on-activate=true",
    "logging.level.com.sysml.platform.infrastructure.cdo=DEBUG",
    "logging.level.org.eclipse.emf.cdo=DEBUG"
})
class CDOStorageTest {

  @Autowired
  private CDOServerManager cdoServerManager;

  @Test
  void testBasicEMFModelStorage() throws CommitException {
    System.out.println("========== Testing Basic EMF Model Storage ==========");
    
    // 1. 验证CDO服务器健康
    assertThat(cdoServerManager).isNotNull();
    assertThat(cdoServerManager.isHealthy()).isTrue();
    assertThat(cdoServerManager.getRepository().getState()).isEqualTo(IRepository.State.ONLINE);
    
    // 2. 开启CDO事务
    CDOTransaction transaction = cdoServerManager.openTransaction();
    assertThat(transaction).isNotNull();
    
    try {
      // 3. 创建CDO资源
      String resourcePath = "/test/models/" + UUID.randomUUID().toString();
      CDOResource resource = transaction.createResource(resourcePath);
      
      // 4. 创建动态EMF模型定义
      EPackage testPackage = EcoreFactory.eINSTANCE.createEPackage();
      testPackage.setName("TestPackage");
      testPackage.setNsPrefix("test");
      testPackage.setNsURI("http://test/1.0");
      
      // 创建一个EClass
      EClass testClass = EcoreFactory.eINSTANCE.createEClass();
      testClass.setName("TestEntity");
      
      // 添加属性
      EAttribute nameAttr = EcoreFactory.eINSTANCE.createEAttribute();
      nameAttr.setName("name");
      nameAttr.setEType(EcorePackage.Literals.ESTRING);
      testClass.getEStructuralFeatures().add(nameAttr);
      
      EAttribute idAttr = EcoreFactory.eINSTANCE.createEAttribute();
      idAttr.setName("id");
      idAttr.setEType(EcorePackage.Literals.EINT);
      testClass.getEStructuralFeatures().add(idAttr);
      
      testPackage.getEClassifiers().add(testClass);
      
      // 5. 注册包到session
      cdoServerManager.getSession().getPackageRegistry().putEPackage(testPackage);
      
      // 6. 创建模型实例（这会成为CDOObject）
      EFactory factory = testPackage.getEFactoryInstance();
      EObject testInstance = factory.create(testClass);
      testInstance.eSet(nameAttr, "Test Entity 1");
      testInstance.eSet(idAttr, 123);
      
      // 7. 将实例添加到资源（这会使对象变成CDOObject）
      resource.getContents().add(testInstance);
      
      // 提交事务
      transaction.commit();
      System.out.println("✅ Model instance saved to CDO at path: " + resourcePath);
      
      // 8. 验证实例已成为CDOObject
      assertThat(testInstance).isInstanceOf(CDOObject.class);
      CDOObject cdoObject = (CDOObject) testInstance;
      assertThat(cdoObject.cdoState()).isNotNull();
      assertThat(cdoObject.cdoID()).isNotNull();
      System.out.println("CDO ID: " + cdoObject.cdoID() + ", State: " + cdoObject.cdoState());
      
      // 9. 使用新的View读取模型
      CDOView view = cdoServerManager.getSession().openView();
      try {
        CDOResource loadedResource = view.getResource(resourcePath);
        assertThat(loadedResource).isNotNull();
        assertThat(loadedResource.getContents()).hasSize(1);
        
        EObject loadedObject = loadedResource.getContents().get(0);
        assertThat(loadedObject).isInstanceOf(CDOObject.class);
        
        // 验证加载的对象数据
        assertThat(loadedObject.eGet(nameAttr)).isEqualTo("Test Entity 1");
        assertThat(loadedObject.eGet(idAttr)).isEqualTo(123);
        
        CDOObject loadedCDOObject = (CDOObject) loadedObject;
        System.out.println("Loaded CDO Object ID: " + loadedCDOObject.cdoID());
        
        System.out.println("✅ Model successfully loaded from CDO");
        
      } finally {
        view.close();
      }
      
    } finally {
      transaction.close();
    }
    
    System.out.println("========== EMF Model Storage Test PASSED ==========");
  }

  @Test
  void testModelInstanceStorage() throws CommitException {
    System.out.println("========== Testing Model Instance Storage ==========");
    
    // 1. 开启CDO事务
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      // 2. 创建CDO资源
      String resourcePath = "/test/instances/" + UUID.randomUUID().toString();
      CDOResource resource = transaction.createResource(resourcePath);
      
      // 3. 创建动态EMF模型定义
      EPackage dynamicPackage = EcoreFactory.eINSTANCE.createEPackage();
      dynamicPackage.setName("DynamicPackage");
      dynamicPackage.setNsPrefix("dyn");
      dynamicPackage.setNsURI("http://dynamic/1.0");
      
      EClass personClass = EcoreFactory.eINSTANCE.createEClass();
      personClass.setName("Person");
      
      EAttribute nameAttr = EcoreFactory.eINSTANCE.createEAttribute();
      nameAttr.setName("name");
      nameAttr.setEType(EcorePackage.Literals.ESTRING);
      personClass.getEStructuralFeatures().add(nameAttr);
      
      EAttribute ageAttr = EcoreFactory.eINSTANCE.createEAttribute();
      ageAttr.setName("age");
      ageAttr.setEType(EcorePackage.Literals.EINT);
      personClass.getEStructuralFeatures().add(ageAttr);
      
      dynamicPackage.getEClassifiers().add(personClass);
      
      // 4. 注册包到session
      cdoServerManager.getSession().getPackageRegistry().putEPackage(dynamicPackage);
      
      // 5. 创建模型实例
      EFactory factory = dynamicPackage.getEFactoryInstance();
      EObject person1 = factory.create(personClass);
      person1.eSet(nameAttr, "Alice");
      person1.eSet(ageAttr, 30);
      
      EObject person2 = factory.create(personClass);
      person2.eSet(nameAttr, "Bob");
      person2.eSet(ageAttr, 25);
      
      // 6. 添加实例到资源（这会使它们变成CDOObject）
      resource.getContents().add(person1);
      resource.getContents().add(person2);
      
      // 提交
      transaction.commit();
      System.out.println("✅ Model instances saved to CDO");
      
      // 7. 验证CDO状态
      assertThat(person1).isInstanceOf(CDOObject.class);
      assertThat(person2).isInstanceOf(CDOObject.class);
      
      // 8. 读取并验证
      CDOView view = cdoServerManager.getSession().openView();
      try {
        CDOResource loadedResource = view.getResource(resourcePath);
        assertThat(loadedResource.getContents()).hasSize(2);
        
        EObject loadedPerson1 = loadedResource.getContents().get(0);
        assertThat(loadedPerson1.eGet(nameAttr)).isEqualTo("Alice");
        assertThat(loadedPerson1.eGet(ageAttr)).isEqualTo(30);
        
        EObject loadedPerson2 = loadedResource.getContents().get(1);
        assertThat(loadedPerson2.eGet(nameAttr)).isEqualTo("Bob");
        assertThat(loadedPerson2.eGet(ageAttr)).isEqualTo(25);
        
        System.out.println("✅ Model instances successfully loaded from CDO");
        
      } finally {
        view.close();
      }
      
    } finally {
      transaction.close();
    }
    
    System.out.println("========== Model Instance Storage Test PASSED ==========");
  }

  @Test
  void testConcurrentAccess() throws Exception {
    System.out.println("========== Testing Concurrent CDO Access ==========");
    
    String resourcePath = "/test/concurrent/" + UUID.randomUUID().toString();
    
    // 1. 用户1创建资源
    CDOTransaction tx1 = cdoServerManager.openTransaction();
    try {
      CDOResource resource = tx1.createResource(resourcePath);
      
      EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
      pkg.setName("SharedPackage");
      pkg.setNsURI("http://shared/1.0");
      resource.getContents().add(pkg);
      
      tx1.commit();
      System.out.println("User 1: Created resource");
      
      // 2. 用户2读取并修改
      CDOTransaction tx2 = cdoServerManager.getSession().openTransaction();
      try {
        CDOResource resource2 = tx2.getResource(resourcePath);
        EPackage pkg2 = (EPackage) resource2.getContents().get(0);
        pkg2.setNsPrefix("shared");
        
        tx2.commit();
        System.out.println("User 2: Modified package");
        
        // 3. 用户1需要更新才能看到变化
        tx1.waitForUpdate(tx2.getLastCommitTime(), 5000);
        
        assertThat(pkg.getNsPrefix()).isEqualTo("shared");
        System.out.println("User 1: Received update from User 2");
        
      } finally {
        tx2.close();
      }
      
    } finally {
      tx1.close();
    }
    
    System.out.println("========== Concurrent Access Test PASSED ==========");
  }

  @Test 
  void testLargeModelStorage() throws CommitException {
    System.out.println("========== Testing Large Model Storage ==========");
    
    CDOTransaction transaction = cdoServerManager.openTransaction();
    try {
      String resourcePath = "/test/large/" + UUID.randomUUID().toString();
      CDOResource resource = transaction.createResource(resourcePath);
      
      // 创建一个包含多个类的大模型
      EPackage largePackage = EcoreFactory.eINSTANCE.createEPackage();
      largePackage.setName("LargePackage");
      largePackage.setNsURI("http://large/1.0");
      
      // 创建100个类，每个类10个属性
      for (int i = 0; i < 100; i++) {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Class" + i);
        
        for (int j = 0; j < 10; j++) {
          EAttribute attr = EcoreFactory.eINSTANCE.createEAttribute();
          attr.setName("attr" + j);
          attr.setEType(EcorePackage.Literals.ESTRING);
          eClass.getEStructuralFeatures().add(attr);
        }
        
        largePackage.getEClassifiers().add(eClass);
      }
      
      resource.getContents().add(largePackage);
      
      long startTime = System.currentTimeMillis();
      transaction.commit();
      long commitTime = System.currentTimeMillis() - startTime;
      
      System.out.println("✅ Large model (100 classes, 1000 attributes) saved in " + commitTime + " ms");
      assertThat(commitTime).isLessThan(5000); // 应该在5秒内完成
      
      // 验证读取
      CDOView view = cdoServerManager.getSession().openView();
      try {
        startTime = System.currentTimeMillis();
        CDOResource loadedResource = view.getResource(resourcePath);
        EPackage loadedPackage = (EPackage) loadedResource.getContents().get(0);
        long loadTime = System.currentTimeMillis() - startTime;
        
        assertThat(loadedPackage.getEClassifiers()).hasSize(100);
        System.out.println("✅ Large model loaded in " + loadTime + " ms");
        assertThat(loadTime).isLessThan(2000); // 加载应该更快
        
      } finally {
        view.close();
      }
      
    } finally {
      transaction.close();
    }
    
    System.out.println("========== Large Model Storage Test PASSED ==========");
  }
}