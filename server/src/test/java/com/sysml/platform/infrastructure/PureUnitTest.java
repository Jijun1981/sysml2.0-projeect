package com.sysml.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sysml.platform.infrastructure.emf.ModelFormatAdapter;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 纯单元测试 - 不使用Spring，直接测试组件
 *
 * <p>这个测试展示了基础设施组件的核心功能是正常的
 */
public class PureUnitTest {

  private ModelFormatAdapter formatAdapter;

  @BeforeEach
  void setUp() {
    // 直接创建组件，不依赖Spring
    formatAdapter = new ModelFormatAdapter();
    // ModelFormatAdapter使用@PostConstruct，手动调用不需要
  }

  @Test
  void testXMIRoundTripWithoutSpring() throws Exception {
    // 创建测试模型
    EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
    pkg.setName("PureTestPackage");
    pkg.setNsURI("http://pure.test/1.0");
    pkg.setNsPrefix("pt");

    // 添加类
    EClass cls = EcoreFactory.eINSTANCE.createEClass();
    cls.setName("PureTestClass");
    pkg.getEClassifiers().add(cls);

    // 执行XMI往返测试
    boolean result = formatAdapter.validateXMIRoundTrip(pkg);
    assertThat(result).isTrue();
  }

  @Test
  void testJSONRoundTripWithoutSpring() throws Exception {
    // 创建测试模型
    EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
    pkg.setName("JSONPureTestPackage");
    pkg.setNsURI("http://json.pure.test/1.0");
    pkg.setNsPrefix("jpt");

    // 执行JSON往返测试
    boolean result = formatAdapter.validateJSONRoundTrip(pkg);
    assertThat(result).isTrue();
  }

  @Test
  void testEPackageCreation() {
    // 测试EMF基本功能
    EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
    pkg.setName("TestPackage");
    pkg.setNsURI("http://test/1.0");

    assertThat(pkg.getName()).isEqualTo("TestPackage");
    assertThat(pkg.getNsURI()).isEqualTo("http://test/1.0");

    // 创建类
    EClass eClass = EcoreFactory.eINSTANCE.createEClass();
    eClass.setName("TestClass");
    pkg.getEClassifiers().add(eClass);

    assertThat(pkg.getEClassifiers()).hasSize(1);
    assertThat(pkg.getEClassifiers().get(0).getName()).isEqualTo("TestClass");
  }

  @Test
  void testEObjectCreation() {
    // 创建包和类
    EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
    pkg.setName("ObjectTestPackage");
    pkg.setNsURI("http://object.test/1.0");

    EClass eClass = EcoreFactory.eINSTANCE.createEClass();
    eClass.setName("Entity");
    pkg.getEClassifiers().add(eClass);

    // 注册包
    EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);

    // 创建实例
    EObject instance = pkg.getEFactoryInstance().create(eClass);
    assertThat(instance).isNotNull();
    assertThat(instance.eClass()).isEqualTo(eClass);
  }
}
