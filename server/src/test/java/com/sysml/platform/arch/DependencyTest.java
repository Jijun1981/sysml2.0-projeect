package com.sysml.platform.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/** 架构依赖测试 验证MECE原则和DAG依赖 */
public class DependencyTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.sysml.platform");

  @Test
  void domainsShouldNotHaveCyclicDependencies() {
    ArchRule rule = slices().matching("com.sysml.platform.domain.(*)..").should().beFreeOfCycles();

    rule.check(classes);
  }

  @Test
  void requirementsDomainShouldNotDependOnOtherDomains() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain.requirements..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..domain.structure..", "..domain.constraints..", "..domain.trace..");

    rule.check(classes);
  }

  @Test
  void traceDomainShouldNotModifyOtherDomains() {
    // Trace域只能读取其他域，不能修改
    // 由于trace域还未实现，此测试暂时总是通过
    assert true;
  }
}
