package com.sysml.platform.domain.requirements;

/**
 * 更新需求输入
 */
public class UpdateRequirementInput {
    private String name;
    private String text;
    private RequirementPriority priority;
    private RequirementStatus status;
    
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
}