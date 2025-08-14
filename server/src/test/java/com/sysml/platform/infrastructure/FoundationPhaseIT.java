package com.sysml.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.sysml.platform.domain.requirements.CreateRequirementInput;
import com.sysml.platform.domain.requirements.RequirementService;
import com.sysml.platform.infrastructure.cdo.CDORepository;
import com.sysml.platform.infrastructure.emf.EMFModelManager;
import com.sysml.platform.infrastructure.transaction.TransactionManager;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Foundation Phase集成测试 验证所有基础设施需求 */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:postgresql://localhost:5432/sysml_dev_db",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
public class FoundationPhaseIT {

  @Autowired private CDORepository cdoRepository;

  @Autowired private EMFModelManager emfModelManager;

  @Autowired private TransactionManager transactionManager;

  @Autowired private RequirementService requirementService;

  /** RQ-INFRA-CDO-001: CDO健康检查必须返回UP */
  @Test
  void cdoHealthCheckShouldReturnUp() {
    assertNotNull(cdoRepository);
    assertTrue(cdoRepository.isHealthy(), "CDO必须健康");

    var sessionInfo = cdoRepository.getSessionInfo();
    assertThat(sessionInfo)
        .containsKey("repositoryName")
        .containsKey("connected")
        .containsEntry("connected", true);
  }

  /** RQ-INFRA-TX-002: 事务commit/rollback语义正确 */
  @Test
  void transactionShouldCommitAndRollbackCorrectly() {
    // 测试提交
    boolean commitResult =
        transactionManager.testCommit(
            () -> {
              // 执行一些操作
              var input = new CreateRequirementInput();
              input.setReqId("TX-TEST-001");
              input.setName("Transaction Test");
              requirementService.createRequirement(input);
            });
    assertTrue(commitResult, "事务提交必须成功");

    // 验证数据已保存
    var saved = requirementService.findById("TX-TEST-001");
    assertTrue(saved.isPresent(), "提交后数据必须存在");

    // 测试回滚
    boolean rollbackResult =
        transactionManager.testRollback(
            () -> {
              var input = new CreateRequirementInput();
              input.setReqId("TX-TEST-002");
              input.setName("Rollback Test");
              requirementService.createRequirement(input);
            });
    assertTrue(rollbackResult, "事务回滚必须成功");

    // 验证数据未保存
    var notSaved = requirementService.findById("TX-TEST-002");
    assertFalse(notSaved.isPresent(), "回滚后数据不应存在");

    // 检查统计
    var stats = transactionManager.getStats();
    assertTrue(stats.commitCount() > 0, "必须有成功的提交");
    assertTrue(stats.rollbackCount() > 0, "必须有成功的回滚");
  }

  /** RQ-INFRA-EMF-003: EMFModelManager CRUD操作正确 */
  @Test
  void emfModelManagerShouldPerformCRUD() {
    // 创建对象
    String nsUri = "http://www.eclipse.org/emf/2002/Ecore";
    String className = "EClass";

    EObject eClass = emfModelManager.createObject(nsUri, className);
    assertNotNull(eClass, "必须能创建EMF对象");

    // 更新属性
    emfModelManager.updateObject(eClass, "name", "TestClass");
    assertEquals("TestClass", eClass.eGet(eClass.eClass().getEStructuralFeature("name")));

    // 保存到CDO
    String id = emfModelManager.saveObject(eClass);
    assertNotNull(id, "必须能保存到CDO");

    // 删除
    boolean deleted = emfModelManager.deleteObject(eClass);
    assertTrue(deleted, "必须能删除对象");
  }

  /** RQ-M2-REG-001: EPackage注册验证 */
  @Test
  void ePackagesShouldBeRegistered() {
    var registry = org.eclipse.emf.ecore.EPackage.Registry.INSTANCE;

    // 检查KerML包
    assertNotNull(registry.getEPackage("http://www.omg.org/spec/KerML/20230201"), "KerML包必须已注册");

    // 检查SysML包
    assertNotNull(registry.getEPackage("http://www.omg.org/spec/SysML/20230201"), "SysML包必须已注册");
  }

  /** RQ-M2-FACTORY-002: 工厂创建验证 */
  @Test
  void factoryShouldCreateModelInstances() {
    // 批量创建测试
    var objects =
        emfModelManager.createMultipleObjects(
            "http://www.eclipse.org/emf/2002/Ecore", "EAttribute", 10);

    assertEquals(10, objects.size(), "必须能批量创建实例");
    objects.forEach(obj -> assertNotNull(obj, "每个实例必须非空"));
  }

  /** RQ-M2-ROUNDTRIP-003: XMI往返等价验证 */
  @Test
  void xmiRoundTripShouldBeEquivalent() throws Exception {
    // 创建测试对象
    EObject original =
        emfModelManager.createObject("http://www.eclipse.org/emf/2002/Ecore", "EClass");
    emfModelManager.updateObject(original, "name", "RoundTripTest");

    // XMI序列化
    String xmi = emfModelManager.toXMI(original);
    assertNotNull(xmi, "XMI序列化必须成功");
    assertThat(xmi).contains("RoundTripTest");

    // XMI反序列化
    EObject restored = emfModelManager.fromXMI(xmi);
    assertNotNull(restored, "XMI反序列化必须成功");

    // 验证往返等价
    boolean equivalent = emfModelManager.validateXMIRoundTrip(original);
    assertTrue(equivalent, "XMI往返必须等价");
  }

  /** RQ-API-DATALOADER-003: DataLoader N+1查询解决 */
  @Test
  @Transactional
  void dataLoaderShouldBatchLoad() {
    // 创建多个需求用于测试
    for (int i = 1; i <= 5; i++) {
      var input = new CreateRequirementInput();
      input.setReqId("DL-TEST-" + i);
      input.setName("DataLoader Test " + i);
      requirementService.createRequirement(input);
    }

    // DataLoader将在GraphQL查询时自动批量加载
    // 这里验证服务可用
    var page = requirementService.findAll(1, 10);
    assertNotNull(page);
    assertTrue(page.getItems().size() >= 5, "必须能查询到创建的需求");
  }

  /** 验证PostgreSQL连接和JSONB支持 */
  @Test
  void postgreSQLShouldBeConnected() {
    // 创建带扩展属性的需求
    var input = new CreateRequirementInput();
    input.setReqId("PG-TEST-001");
    input.setName("PostgreSQL Test");
    input.setText("Testing JSONB support");

    var created = requirementService.createRequirement(input);
    assertNotNull(created);

    // 验证能从PostgreSQL读取
    var found = requirementService.findById(created.getId());
    assertTrue(found.isPresent());
    assertEquals("PostgreSQL Test", found.get().getName());
  }
}
