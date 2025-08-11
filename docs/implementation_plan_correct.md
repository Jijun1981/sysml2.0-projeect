# 正确的实施计划 - 基于追踪矩阵

## 当前问题分析
1. **违反依赖顺序**：直接开发了EP-REQ（需求域），但其依赖的EP-M2-PILOT还未实现
2. **基础不牢**：EMF框架只是占位，没有真正集成SysML v2的M2模型
3. **错误的实现**：RequirementService使用了普通Java类，而不是基于EMF的EObject

## 正确的开发顺序

### Phase 0: Foundation（必须100%完成）

#### 1. EP-INFRA：基础设施（当前20%）
- [x] RQ-INFRA-CDO-001: CDO健康检查（已完成，但是假的）
- [ ] RQ-INFRA-TX-002: 事务边界管理
- [x] RQ-INFRA-EMF-003: EMFModelManager（已完成，但没用EMF）

**问题**：需要真正集成CDO和EMF，而不是用Map模拟

#### 2. EP-M2-PILOT：M2模型复用（当前0%）⚠️ **最关键缺失**
- [ ] RQ-M2-REG-001: 注册KerML/SysML的EPackage
- [ ] RQ-M2-FACTORY-002: 使用工厂创建SysML元素
- [ ] RQ-M2-ROUNDTRIP-003: XMI/JSON往返转换

**需要做的**：
1. 加载`/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore`
2. 生成Java代码或动态加载
3. 注册到EPackage.Registry
4. 实现RequirementDefinition extends EObject

#### 3. EP-API：API契约层（当前30%）
- [x] RQ-API-ENDPOINT-001: GraphQL端点（部分）
- [ ] RQ-API-CORE-002: 核心契约
- [ ] RQ-API-DATALOADER-003: DataLoader配置
- [ ] RQ-API-SNAPSHOT-004: Schema快照

#### 4. EP-UI-BASE：UI基础（当前0%）
- [ ] RQ-UI-RUNTIME-001: Sirius运行时绑定
- [ ] RQ-UI-VIEWS-002: 视图类型

#### 5. EP-NFR：横切关注点（当前20%）
- [ ] RQ-NFR-HEALTH-001: 健康检查聚合
- [ ] RQ-NFR-METRICS-002: 指标暴露
- [ ] RQ-NFR-LOG-003: 结构化日志
- [ ] RQ-NFR-ERROR-004: 错误码注册
- [ ] RQ-NFR-AUTH-005: 鉴权模式

### Phase 1: P1需求域（依赖Foundation完成）
只有当Foundation 100%完成后，才能开始EP-REQ，因为：
- 需要EMF的EObject作为基类
- 需要SysML的RequirementDefinition元模型
- 需要CDO进行持久化

## 立即行动计划

### Step 1: 集成真正的EMF框架（EP-M2-PILOT）
```java
// 1. 加载SysML.ecore
Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
ResourceSet resourceSet = new ResourceSetImpl();
Resource resource = resourceSet.getResource(URI.createFileURI("model/SysML.ecore"), true);
EPackage sysmlPackage = (EPackage) resource.getContents().get(0);

// 2. 注册包
EPackage.Registry.INSTANCE.put(sysmlPackage.getNsURI(), sysmlPackage);

// 3. 获取RequirementDefinition的EClass
EClass requirementClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
```

### Step 2: 重构RequirementService使用EMF
```java
// 不再是普通Java类
public class RequirementService {
    @Autowired
    private CDORepository repository;
    
    public EObject createRequirement(Map<String, Object> input) {
        // 使用EMF工厂创建
        EObject requirement = EcoreUtil.create(requirementClass);
        requirement.eSet(nameAttribute, input.get("name"));
        // ...
        
        // 通过CDO事务保存
        CDOTransaction transaction = repository.openTransaction();
        transaction.getOrCreateResource("/requirements").getContents().add(requirement);
        transaction.commit();
        
        return requirement;
    }
}
```

### Step 3: 配置CDO服务器
```java
@Configuration
public class CDOConfiguration {
    @Bean
    public IRepository createRepository() {
        IStore store = createStore();
        Map<String, String> props = new HashMap<>();
        props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo");
        
        IRepository repository = CDOServerUtil.createRepository(
            "sysml-repo", store, props);
        
        // 注册SysML包
        repository.getPackageRegistry().put(
            SysMLPackage.eINSTANCE.getNsURI(), 
            SysMLPackage.eINSTANCE);
        
        return repository;
    }
}
```

## 时间线
1. **立即停止**：EP-REQ的开发
2. **Week 1**：完成EP-M2-PILOT（加载和注册M2模型）
3. **Week 2**：完成EP-INFRA（真正的CDO/EMF集成）
4. **Week 3**：完成EP-API（GraphQL与EMF绑定）
5. **Week 4**：重构EP-REQ，基于正确的EMF基础

## 关键依赖链
```
EP-M2-PILOT (SysML.ecore)
    ↓
EP-INFRA (CDO + EMF)
    ↓
EP-API (GraphQL)
    ↓
EP-REQ (需求域) ← 现在才能开始！
```

## 结论
必须回到Foundation阶段，特别是EP-M2-PILOT的实现。没有M2模型，后续所有开发都是错误的。