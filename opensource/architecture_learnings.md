# SysML v2 API 架构学习要点

## 1. 泛型基类模式（减少重复代码）

```java
// 基础Service泛型类
public abstract class BaseService<E, D extends Dao<E>> {
    protected final D dao;
    
    public Optional<E> getById(UUID id) {
        return dao.findById(id);
    }
}

// 具体Service只需关注业务逻辑
public class ProjectService extends BaseService<Project, ProjectDao> {
    // 专注于Project特有的业务逻辑
}
```

## 2. 函数式事务管理

```java
// 优雅的事务处理
public interface JPAManager {
    <R> R transact(Function<EntityManager, R> function);
}

// 使用示例
Optional<Project> project = jpaManager.transact(em -> {
    em.persist(newProject);
    return Optional.of(newProject);
});
```

## 3. 基于游标的分页（高性能）

```java
// 不使用offset/limit，而是基于游标
public Page<E> findAll(UUID after, UUID before, int maxResults) {
    // 基于UUID的游标分页，避免深度分页性能问题
}
```

## 4. JSON-LD支持（语义Web）

```java
// 根据Accept头自动选择格式
if (request.accepts("application/ld+json")) {
    return jsonLd(entity);
} else {
    return json(entity);
}
```

## 5. 依赖注入最佳实践

```java
// 构造器注入 + 单例
@Singleton
public class ProjectService {
    private final ProjectDao dao;
    
    @Inject
    public ProjectService(ProjectDao dao) {
        this.dao = dao;
    }
}
```

## 6. 元模型运行时发现

```java
// 使用反射自动发现所有元模型类
Reflections reflections = new Reflections("org.omg.sysml");
Set<Class<? extends Data>> implementations = 
    reflections.getSubTypesOf(Data.class);
```

## 7. 可以直接借鉴的部分

### a. Controller基类设计
```java
public abstract class BaseController {
    // 统一的错误处理
    protected Result handleError(Exception e) {
        return internalServerError(Json.toJson(
            Map.of("error", e.getMessage())
        ));
    }
    
    // 统一的分页响应
    protected Result paginated(Page<?> page) {
        return ok(Json.toJson(page))
            .withHeader("X-Total-Count", String.valueOf(page.total));
    }
}
```

### b. DAO层查询构建器模式
```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Project> query = cb.createQuery(Project.class);
Root<Project> root = query.from(Project.class);

// 动态构建查询条件
List<Predicate> predicates = new ArrayList<>();
if (after != null) {
    predicates.add(cb.greaterThan(root.get("id"), after));
}
query.where(predicates.toArray(new Predicate[0]));
```

### c. 实体ID生成策略
```java
// 使用UUID作为主键
@Id
@GeneratedValue(generator = "uuid2")
@GenericGenerator(name = "uuid2", strategy = "uuid2")
private UUID elementId;
```

## 8. 架构改进建议（对你们的系统）

### 如果你们"多做一层PostgreSQL"的意思是：

**方案A：API层 + 业务数据层**
```
REST API DB (元数据、配置)
    ↓
业务数据DB (实际模型数据)
```
建议：使用不同的schema而不是不同的数据库

**方案B：主从复制**
```
主库（写）→ 从库（读）
```
建议：可以在DAO层实现读写分离

**方案C：缓存层**
```
API → Redis缓存 → PostgreSQL
```
建议：可以用Spring Cache抽象

## 9. 性能优化技巧

1. **延迟加载配置**
```java
@ManyToMany(fetch = FetchType.LAZY)
private List<Feature> features;
```

2. **批量操作**
```java
@Modifying
@Query("DELETE FROM Element e WHERE e.project = :project")
void deleteByProject(@Param("project") Project project);
```

3. **连接池配置**
```xml
<property name="hibernate.c3p0.min_size" value="5"/>
<property name="hibernate.c3p0.max_size" value="20"/>
```

## 10. 关键启发

1. **不要过度设计Service层** - SysML v2的Service层很薄，主要逻辑在DAO
2. **泛型是你的朋友** - 大量使用泛型减少代码重复
3. **函数式编程思想** - 事务管理使用函数式接口
4. **元模型不一定需要EMF** - 纯Java也能实现复杂元模型
5. **分页用游标不用offset** - 性能更好，特别是大数据集

## 实践建议

对于你们的项目，可以：
1. 直接复用BaseController/BaseService/BaseDao的设计
2. 采用相同的依赖注入模式
3. 使用类似的事务管理策略
4. 考虑支持JSON-LD以增强语义表达能力