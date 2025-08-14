/*
 * 修复application/ld+json Content-Type问题的补丁
 * 
 * 问题分析：
 * 1. 当Content-Type是application/ld+json时，metamodelProvider.getImplementationClass返回null
 * 2. 导致Jackson的fromJson方法抛出NullPointerException
 * 
 * 解决方案：
 * 1. 在所有控制器的post方法中添加null检查
 * 2. 或者修复MetamodelProvider的实现
 * 3. 或者在请求处理前统一处理Content-Type
 */

// 方案1: 修改ProjectController.java第98行附近
public Result postProject(Request request) {
    JsonNode requestBodyJson = request.body().asJson();
    
    // 添加null检查和默认值处理
    Class<? extends Project> implementationClass = metamodelProvider.getImplementationClass(Project.class);
    if (implementationClass == null) {
        // 如果是JSON-LD，尝试直接使用实现类
        implementationClass = ProjectImpl.class;
    }
    
    Project requestedObject = Json.fromJson(requestBodyJson, implementationClass);
    if (requestedObject.getId() != null || requestedObject.getCreated() != null) {
        return Results.badRequest();
    }
    requestedObject.setCreated(ZonedDateTime.now());
    Optional<Project> project = projectService.create(requestedObject);
    if (project.isEmpty()) {
        return Results.internalServerError();
    }
    return buildResult(project.get(), request, null);
}

// 方案2: 修改JPAMetamodelProvider以支持Project类
// 在JPAMetamodelProvider.java的static块中添加：
static {
    List<Class<?>> roots = Arrays.asList(
        Data.class,
        Record.class,
        Constraint.class,
        Project.class  // 添加Project作为根类型
    );
    // ...
}

// 方案3: 创建一个请求过滤器处理Content-Type
public class ContentTypeNormalizer {
    public static Request normalizeContentType(Request request) {
        String contentType = request.contentType().orElse("");
        if (contentType.contains("application/ld+json")) {
            // 将JSON-LD请求当作普通JSON处理
            return request.withHeader("Content-Type", "application/json");
        }
        return request;
    }
}