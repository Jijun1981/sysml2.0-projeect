package com.sysml.platform.api.graphql;

import com.sysml.platform.domain.requirements.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.HashMap;

/**
 * 需求域GraphQL解析器
 * 实现EP-REQ相关的Query和Mutation
 * @implements RQ-REQ-SCHEMA-000
 */
@Controller
public class RequirementResolver {
    
    private final RequirementService requirementService;
    
    public RequirementResolver(RequirementService requirementService) {
        this.requirementService = requirementService;
    }
    
    @QueryMapping
    public Object requirement(@Argument String id) {
        // 实现单个需求查询
        return requirementService.queryRequirements(
            QueryRequirementsInput.builder()
                .keyword(id)
                .page(0)
                .size(1)
                .build()
        ).getContent().stream().findFirst().orElse(null);
    }
    
    @QueryMapping
    public Object requirements(@Argument Integer page, @Argument Integer size) {
        // 查询需求列表
        return requirementService.queryRequirements(
            QueryRequirementsInput.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 20)
                .build()
        );
    }
    
    @QueryMapping
    public Object requirementTree(@Argument String rootId) {
        // 查询需求层次树
        return requirementService.queryRequirements(
            QueryRequirementsInput.builder()
                .keyword(rootId)
                .page(0)
                .size(1)
                .build()
        ).getContent().stream().findFirst().orElse(null);
    }
    
    @MutationMapping
    public Object createRequirement(@Argument Map<String, Object> input) {
        // 转换输入
        CreateRequirementInput createInput = CreateRequirementInput.builder()
            .reqId((String) input.get("reqId"))
            .name((String) input.get("name"))
            .text((String) input.get("text"))
            .kind(RequirementKind.valueOf((String) input.get("kind")))
            .priority(input.get("priority") != null ? 
                RequirementPriority.valueOf((String) input.get("priority")) : null)
            .build();
        
        return requirementService.createRequirement(createInput);
    }
    
    @MutationMapping
    public Map<String, Object> updateRequirement(@Argument String id, @Argument Map<String, Object> input) {
        // TODO: 实现更新逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("requirement", requirement(id));
        return result;
    }
    
    @MutationMapping
    public Map<String, Object> deleteRequirement(@Argument String id) {
        // TODO: 实现删除逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }
    
    @MutationMapping
    public Object deriveRequirement(@Argument String sourceId, @Argument String targetId) {
        return requirementService.deriveRequirement(sourceId, targetId);
    }
    
    @MutationMapping
    public Object refineRequirement(@Argument String sourceId, @Argument String targetId) {
        return requirementService.refineRequirement(sourceId, targetId);
    }
}