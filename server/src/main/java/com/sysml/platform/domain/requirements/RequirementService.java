package com.sysml.platform.domain.requirements;

import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 需求域服务
 * 职责：需求CRUD、层次管理、查询
 * 不负责：跨域关系、数值计算
 * 
 * @implements RQ-REQ-CRUD-001
 * @implements RQ-REQ-HIERARCHY-003
 */
@Service
public class RequirementService {
    
    private final Map<String, RequirementDefinition> repository = new HashMap<>();
    private final Set<String> reqIds = new HashSet<>();
    private final Map<String, Set<String>> deriveRelations = new HashMap<>(); // source -> targets
    private final Map<String, Set<String>> refineRelations = new HashMap<>(); // abstract -> refined
    
    public CreateRequirementPayload createRequirement(CreateRequirementInput input) {
        // 验证必填字段
        if (input.getName() == null || input.getKind() == null) {
            return CreateRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("VALIDATION_ERROR")
                    .messageKey("error.validation.required")
                    .build())
                .build();
        }
        
        // 检查reqId唯一性
        if (reqIds.contains(input.getReqId())) {
            return CreateRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_ID_DUPLICATE")
                    .messageKey("error.req.duplicate")
                    .build())
                .build();
        }
        
        // 创建需求
        String id = UUID.randomUUID().toString();
        RequirementDefinition req = RequirementDefinition.builder()
            .id(id)
            .reqId(input.getReqId())
            .name(input.getName())
            .text(input.getText())
            .kind(input.getKind())
            .priority(input.getPriority())
            .status(RequirementStatus.DRAFT)
            .createdAt(LocalDateTime.now().toString())
            .createdBy("system")
            .build();
        
        repository.put(id, req);
        reqIds.add(input.getReqId());
        
        return CreateRequirementPayload.builder()
            .ok(true)
            .requirement(req)
            .build();
    }
    
    /**
     * 建立derive关系（派生）
     * @implements RQ-REQ-HIERARCHY-003
     */
    public DeriveRequirementPayload deriveRequirement(String sourceId, String targetId) {
        // 检查需求是否存在
        RequirementDefinition source = repository.get(sourceId);
        RequirementDefinition target = repository.get(targetId);
        
        if (source == null || target == null) {
            return DeriveRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_NOT_FOUND")
                    .messageKey("error.req.notfound")
                    .build())
                .build();
        }
        
        // 检测是否会形成环
        if (wouldCreateCycle(sourceId, targetId)) {
            return DeriveRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_CYCLE_DETECTED")
                    .messageKey("error.req.cycle")
                    .build())
                .build();
        }
        
        // 建立关系
        deriveRelations.computeIfAbsent(sourceId, k -> new HashSet<>()).add(targetId);
        
        // 更新需求对象中的关系
        Set<String> derivedFrom = source.getDerivedFrom();
        if (derivedFrom == null) {
            derivedFrom = new HashSet<>();
            source.setDerivedFrom(derivedFrom);
        }
        derivedFrom.add(targetId);
        
        return DeriveRequirementPayload.builder()
            .ok(true)
            .source(source)
            .target(target)
            .build();
    }
    
    /**
     * 查询需求
     * @implements RQ-REQ-QUERY-004
     */
    public QueryRequirementsPayload queryRequirements(QueryRequirementsInput input) {
        // 使用索引优化查询
        List<RequirementDefinition> filtered = new ArrayList<>();
        String keyword = input.getKeyword();
        
        // 如果有关键字，执行过滤
        if (keyword != null && !keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            for (RequirementDefinition req : repository.values()) {
                if (matchesKeyword(req, lowerKeyword)) {
                    filtered.add(req);
                }
            }
        } else {
            // 没有关键字，返回所有
            filtered.addAll(repository.values());
        }
        
        // 排序
        filtered.sort((a, b) -> {
            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt()); // 按创建时间倒序
            }
            return 0;
        });
        
        // 分页
        int page = input.getPage() != null ? input.getPage() : 0;
        int size = input.getSize() != null ? input.getSize() : 20;
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        
        List<RequirementDefinition> content = new ArrayList<>();
        if (start < filtered.size()) {
            content = filtered.subList(start, end);
        }
        
        return QueryRequirementsPayload.builder()
            .ok(true)
            .content(content)
            .totalElements(filtered.size())
            .totalPages((filtered.size() + size - 1) / size)
            .pageNumber(page)
            .pageSize(size)
            .build();
    }
    
    private boolean matchesKeyword(RequirementDefinition req, String keyword) {
        // 在多个字段中搜索
        if (req.getName() != null && req.getName().toLowerCase().contains(keyword)) {
            return true;
        }
        if (req.getText() != null && req.getText().toLowerCase().contains(keyword)) {
            return true;
        }
        if (req.getReqId() != null && req.getReqId().toLowerCase().contains(keyword)) {
            return true;
        }
        return false;
    }
    
    /**
     * 建立refine关系（细化）
     * @implements RQ-REQ-RELATION-005
     */
    public RefineRequirementPayload refineRequirement(String abstractId, String refinedId) {
        // 检查需求是否存在
        RequirementDefinition abstract_ = repository.get(abstractId);
        RequirementDefinition refined = repository.get(refinedId);
        
        if (abstract_ == null || refined == null) {
            return RefineRequirementPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_NOT_FOUND")
                    .messageKey("error.req.notfound")
                    .build())
                .build();
        }
        
        // 建立refine关系
        refineRelations.computeIfAbsent(abstractId, k -> new HashSet<>()).add(refinedId);
        
        // 更新需求对象中的关系
        Set<String> refines = abstract_.getRefines();
        if (refines == null) {
            refines = new HashSet<>();
            abstract_.setRefines(refines);
        }
        refines.add(refinedId);
        
        return RefineRequirementPayload.builder()
            .ok(true)
            .source(abstract_)
            .target(refined)
            .build();
    }
    
    /**
     * 查询需求的所有关系
     * @implements RQ-REQ-RELATION-005
     */
    public RequirementRelationsPayload getRequirementRelations(String requirementId) {
        RequirementDefinition req = repository.get(requirementId);
        
        if (req == null) {
            return RequirementRelationsPayload.builder()
                .ok(false)
                .error(Error.builder()
                    .code("REQ_NOT_FOUND")
                    .messageKey("error.req.notfound")
                    .build())
                .build();
        }
        
        // 收集所有关系
        Set<String> derives = deriveRelations.get(requirementId);
        Set<String> refines = refineRelations.get(requirementId);
        
        return RequirementRelationsPayload.builder()
            .ok(true)
            .requirementId(requirementId)
            .derives(derives != null ? derives : new HashSet<>())
            .refines(refines != null ? refines : new HashSet<>())
            .build();
    }
    
    /**
     * 检测是否会形成环
     * 使用DFS检测从target是否能到达source
     */
    private boolean wouldCreateCycle(String source, String target) {
        Set<String> visited = new HashSet<>();
        return dfs(target, source, visited);
    }
    
    private boolean dfs(String current, String target, Set<String> visited) {
        if (current.equals(target)) {
            return true; // 找到环
        }
        
        if (visited.contains(current)) {
            return false; // 已访问过
        }
        
        visited.add(current);
        
        Set<String> neighbors = deriveRelations.get(current);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (dfs(neighbor, target, visited)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}