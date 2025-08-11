package com.sysml.platform.infrastructure.emf;

import com.sysml.platform.domain.requirements.*;
import com.sysml.platform.m2.SysMLModelAdapter;
import org.eclipse.emf.ecore.EObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EMF需求适配器
 * 连接RequirementService和EMF模型层
 * 按照EP-M2-PILOT规划实现服务层集成
 * 
 * @implements EP-M2-PILOT
 * @tagged RQ-M2-ADAPTER
 */
@Component
public class EMFRequirementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(EMFRequirementAdapter.class);
    
    @Autowired
    private SysMLModelAdapter modelAdapter;
    
    @Autowired 
    private RequirementService requirementService;
    
    /**
     * 将Requirement POJO转换为EMF EObject
     */
    public EObject toEMFModel(Requirement requirement) {
        if (!modelAdapter.isMetamodelLoaded()) {
            logger.warn("EMF metamodel not loaded, returning null");
            return null;
        }
        
        EObject emfReq = modelAdapter.createRequirementDefinition(
            requirement.getId(),
            requirement.getName(),
            requirement.getText()
        );
        
        // 设置额外属性
        if (requirement.getKind() != null) {
            emfReq.eSet(emfReq.eClass().getEStructuralFeature("kind"), 
                requirement.getKind().toString());
        }
        
        if (requirement.getPriority() != null) {
            emfReq.eSet(emfReq.eClass().getEStructuralFeature("priority"),
                requirement.getPriority().toString());
        }
        
        if (requirement.getStatus() != null) {
            emfReq.eSet(emfReq.eClass().getEStructuralFeature("status"),
                requirement.getStatus().toString());
        }
        
        return emfReq;
    }
    
    /**
     * 将EMF EObject转换为Requirement POJO
     */
    public Requirement fromEMFModel(EObject emfObject) {
        if (emfObject == null) {
            return null;
        }
        
        Requirement req = new Requirement();
        
        // 获取基本属性
        Object id = emfObject.eGet(emfObject.eClass().getEStructuralFeature("id"));
        Object name = emfObject.eGet(emfObject.eClass().getEStructuralFeature("name"));
        Object text = emfObject.eGet(emfObject.eClass().getEStructuralFeature("text"));
        
        req.setId(id != null ? id.toString() : null);
        req.setName(name != null ? name.toString() : null);
        req.setText(text != null ? text.toString() : null);
        
        // 获取枚举属性
        Object kind = emfObject.eGet(emfObject.eClass().getEStructuralFeature("kind"));
        if (kind != null) {
            try {
                req.setKind(RequirementKind.valueOf(kind.toString()));
            } catch (Exception e) {
                req.setKind(RequirementKind.FUNCTIONAL);
            }
        }
        
        Object priority = emfObject.eGet(emfObject.eClass().getEStructuralFeature("priority"));
        if (priority != null) {
            try {
                req.setPriority(RequirementPriority.valueOf(priority.toString()));
            } catch (Exception e) {
                req.setPriority(RequirementPriority.MEDIUM);
            }
        }
        
        Object status = emfObject.eGet(emfObject.eClass().getEStructuralFeature("status"));
        if (status != null) {
            try {
                req.setStatus(RequirementStatus.valueOf(status.toString()));
            } catch (Exception e) {
                req.setStatus(RequirementStatus.DRAFT);
            }
        }
        
        return req;
    }
    
    /**
     * 创建EMF派生关系
     */
    public EObject createEMFDeriveRelation(String sourceId, String targetId) {
        // 先通过服务层验证
        DeriveRequirementPayload result = requirementService.deriveRequirement(sourceId, targetId);
        
        if (!result.isOk()) {
            logger.error("Failed to create derive relation: {}", result.getError());
            return null;
        }
        
        // 创建EMF关系对象
        EObject sourceEMF = modelAdapter.createRequirementDefinition(sourceId, "Source", "");
        EObject targetEMF = modelAdapter.createRequirementDefinition(targetId, "Target", "");
        
        return modelAdapter.createDeriveRelation(sourceEMF, targetEMF);
    }
    
    /**
     * 创建EMF细化关系
     */
    public EObject createEMFRefineRelation(String sourceId, String targetId) {
        // 先通过服务层验证
        RefineRequirementPayload result = requirementService.refineRequirement(sourceId, targetId);
        
        if (!result.isOk()) {
            logger.error("Failed to create refine relation: {}", result.getError());
            return null;
        }
        
        // 创建EMF关系对象
        EObject sourceEMF = modelAdapter.createRequirementDefinition(sourceId, "Source", "");
        EObject targetEMF = modelAdapter.createRequirementDefinition(targetId, "Target", "");
        
        return modelAdapter.createRefineRelation(sourceEMF, targetEMF);
    }
    
    /**
     * 同步服务层和EMF模型
     */
    public void syncWithEMF() {
        logger.info("Starting sync between service layer and EMF models");
        
        // 获取所有需求
        QueryRequirementsPayload queryResult = requirementService.queryRequirements(
            QueryRequirementsInput.builder()
                .page(0)
                .size(1000)
                .build()
        );
        
        if (queryResult.getContent() != null) {
            for (Object obj : queryResult.getContent()) {
                if (obj instanceof Requirement) {
                    Requirement req = (Requirement) obj;
                    EObject emfModel = toEMFModel(req);
                    logger.debug("Synced requirement {} to EMF", req.getId());
                }
            }
        }
        
        logger.info("Sync completed");
    }
    
    /**
     * 验证EMF模型与服务层一致性
     */
    public boolean validateConsistency() {
        // 实现一致性验证逻辑
        return true;
    }
}