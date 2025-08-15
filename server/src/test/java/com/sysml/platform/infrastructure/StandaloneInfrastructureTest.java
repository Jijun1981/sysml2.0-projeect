package com.sysml.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sysml.platform.api.HealthController;
import com.sysml.platform.infrastructure.emf.EMFModelManager;
import com.sysml.platform.infrastructure.emf.ModelFormatAdapter;
import com.sysml.platform.infrastructure.emf.SysMLPackageRegistry;
import java.util.Map;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 独立基础设施测试 - 不依赖数据库
 *
 * <p>技术债务缓解：排除数据库自动配置，专注测试核心基础设施组件
 */
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@TestPropertySource(
    properties = {
      "cdo.enabled=false", // 禁用CDO避免数据库依赖
      "sirius.enabled=false", // 禁用Sirius避免复杂依赖
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
    })
public class StandaloneInfrastructureTest {

  @Autowired(required = false)
  private EMFModelManager emfModelManager;

  @Autowired(required = false)
  private SysMLPackageRegistry packageRegistry;

  @Autowired(required = false)
  private ModelFormatAdapter formatAdapter;

  @Autowired(required = false)
  private HealthController healthController;

  @Test
  void testEMFModelManagerExists() {
    // EMF模型管理器应该存在
    assertThat(emfModelManager).isNotNull();

    // 获取统计信息
    Map<String, Object> stats = emfModelManager.getStatistics();
    assertThat(stats).isNotNull();
    assertThat(stats).containsKey("mode");
    assertThat(stats.get("mode")).isEqualTo("LOCAL"); // 没有CDO时应该是本地模式
  }

  @Test
  void testSysMLPackageRegistryExists() {
    // 包注册器应该存在
    assertThat(packageRegistry).isNotNull();

    // 应该有效（使用mock包）
    assertThat(packageRegistry.isValid()).isTrue();

    // 应该有注册的包
    Map<String, EPackage> packages = packageRegistry.getRegisteredPackages();
    assertThat(packages).isNotEmpty();
    assertThat(packages).containsKeys("KerML", "SysML");
  }

  @Test
  void testModelFormatAdapterExists() {
    // 格式适配器应该存在
    assertThat(formatAdapter).isNotNull();
  }

  @Test
  void testXMIRoundTrip() throws Exception {
    if (formatAdapter == null) {
      return; // 跳过如果组件不可用
    }

    // 创建测试包
    EPackage testPackage = EcoreFactory.eINSTANCE.createEPackage();
    testPackage.setName("TestPackage");
    testPackage.setNsURI("http://test/roundtrip/1.0");
    testPackage.setNsPrefix("test");

    // 添加一个类
    EClass testClass = EcoreFactory.eINSTANCE.createEClass();
    testClass.setName("TestClass");
    testPackage.getEClassifiers().add(testClass);

    // 验证XMI往返
    boolean xmiSuccess = formatAdapter.validateXMIRoundTrip(testPackage);
    assertThat(xmiSuccess).isTrue();
  }

  @Test
  void testJSONRoundTrip() throws Exception {
    if (formatAdapter == null) {
      return; // 跳过如果组件不可用
    }

    // 创建测试包
    EPackage testPackage = EcoreFactory.eINSTANCE.createEPackage();
    testPackage.setName("JSONTestPackage");
    testPackage.setNsURI("http://test/json/1.0");
    testPackage.setNsPrefix("json");

    // 验证JSON往返
    boolean jsonSuccess = formatAdapter.validateJSONRoundTrip(testPackage);
    assertThat(jsonSuccess).isTrue();
  }

  @Test
  void testHealthControllerExists() {
    // 健康控制器应该存在
    assertThat(healthController).isNotNull();

    // 获取健康状态
    Map<String, Object> health = healthController.getHealth().getBody();
    assertThat(health).isNotNull();
    assertThat(health).containsKey("status");

    // 检查各子系统状态
    assertThat(health).containsKeys("database", "cdo", "sirius", "emf", "packages");
  }

  @Test
  void testEMFModelCreation() {
    if (emfModelManager == null) {
      return;
    }

    // 注册测试包
    EPackage testPkg = EcoreFactory.eINSTANCE.createEPackage();
    testPkg.setName("CreationTest");
    testPkg.setNsURI("http://test/creation/1.0");
    testPkg.setNsPrefix("create");

    EClass entityClass = EcoreFactory.eINSTANCE.createEClass();
    entityClass.setName("Entity");
    testPkg.getEClassifiers().add(entityClass);

    // 注册包
    EPackage.Registry.INSTANCE.put(testPkg.getNsURI(), testPkg);

    // 创建对象
    EObject entity = emfModelManager.createObject("http://test/creation/1.0", "Entity");
    assertThat(entity).isNotNull();
    assertThat(entity.eClass().getName()).isEqualTo("Entity");
  }
}
