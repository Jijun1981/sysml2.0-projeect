package com.sysml.platform.domain.requirements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 需求实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Requirement {
    private String id;
    private String reqId;
    private String name;
    private String text;
    private RequirementKind kind;
    private RequirementPriority priority;
    private RequirementStatus status;
    
    // 审计字段
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // 层次关系
    private List<Requirement> derivedFrom = new ArrayList<>();
    private List<Requirement> derives = new ArrayList<>();
    private List<Requirement> refinedFrom = new ArrayList<>();
    private List<Requirement> refines = new ArrayList<>();
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getReqId() {
        return reqId;
    }
    
    public void setReqId(String reqId) {
        this.reqId = reqId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public RequirementKind getKind() {
        return kind;
    }
    
    public void setKind(RequirementKind kind) {
        this.kind = kind;
    }
    
    public RequirementPriority getPriority() {
        return priority;
    }
    
    public void setPriority(RequirementPriority priority) {
        this.priority = priority;
    }
    
    public RequirementStatus getStatus() {
        return status;
    }
    
    public void setStatus(RequirementStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public List<Requirement> getDerivedFrom() {
        return derivedFrom;
    }
    
    public void setDerivedFrom(List<Requirement> derivedFrom) {
        this.derivedFrom = derivedFrom;
    }
    
    public List<Requirement> getDerives() {
        return derives;
    }
    
    public void setDerives(List<Requirement> derives) {
        this.derives = derives;
    }
    
    public List<Requirement> getRefinedFrom() {
        return refinedFrom;
    }
    
    public void setRefinedFrom(List<Requirement> refinedFrom) {
        this.refinedFrom = refinedFrom;
    }
    
    public List<Requirement> getRefines() {
        return refines;
    }
    
    public void setRefines(List<Requirement> refines) {
        this.refines = refines;
    }
}