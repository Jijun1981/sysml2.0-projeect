package com.sysml.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sysml.platform.infrastructure.emf.EMFModelManager;
import com.sysml.platform.infrastructure.emf.ModelFormatAdapter;
import com.sysml.platform.infrastructure.emf.SysMLPackageRegistry;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 基础设施单元测试 - 不依赖数据库 */
@SpringBootTest
@ActiveProfiles("test")
class BasicInfrastructureTest {

  @Autowired(required = false)
  private EMFModelManager emfModelManager;

  @Autowired(required = false)
  private SysMLPackageRegistry packageRegistry;

  @Autowired(required = false)
  private ModelFormatAdapter formatAdapter;

  @Test
  void testEMFModelManager() {
    if (emfModelManager == null) {
      System.out.println("EMFModelManager not available");
      return;
    }

    // 测试基本功能
    assertThat(emfModelManager).isNotNull();
    var stats = emfModelManager.getStatistics();
    assertThat(stats).containsKey("mode");
  }

  @Test
  void testPackageRegistry() {
    if (packageRegistry == null) {
      System.out.println("SysMLPackageRegistry not available");
      return;
    }

    assertThat(packageRegistry).isNotNull();
    assertThat(packageRegistry.isValid()).isTrue();

    var packages = packageRegistry.getRegisteredPackages();
    assertThat(packages).isNotEmpty();
  }

  @Test
  void testFormatAdapter() throws Exception {
    if (formatAdapter == null) {
      System.out.println("ModelFormatAdapter not available");
      return;
    }

    // 创建测试对象
    EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
    pkg.setName("TestPackage");
    pkg.setNsURI("http://test/1.0");

    // 测试XMI往返
    boolean xmiPassed = formatAdapter.validateXMIRoundTrip(pkg);
    assertThat(xmiPassed).isTrue();

    // 测试JSON往返
    boolean jsonPassed = formatAdapter.validateJSONRoundTrip(pkg);
    assertThat(jsonPassed).isTrue();
  }
}
