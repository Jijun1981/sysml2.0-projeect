package com.sysml.platform.m2;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SysML元素工厂
 * 实现RQ-M2-FACTORY-002: 工厂创建
 * 使用EMF工厂创建SysML元素实例
 * 
 * @implements RQ-M2-FACTORY-002
 */
@Component
public class SysMLFactory {
    
    @Autowired(required = false)
    private SysMLPackageRegistry registry;
    
    private EFactory sysmlFactory;
    private EFactory kermlFactory;
    
    public SysMLFactory() {
        // 无参构造函数，用于测试
    }
    
    /**
     * 获取注册器
     */
    public SysMLPackageRegistry getRegistry() {
        return registry;
    }
    
    /**
     * 初始化工厂
     */
    public void initialize(String kermlPath, String sysmlPath) {
        if (registry == null) {
            registry = new SysMLPackageRegistry();
        }
        
        // 加载包
        registry.loadKerMLPackage(kermlPath);
        registry.loadAndRegister(sysmlPath);
        
        // 获取工厂
        if (registry.getSysMLPackage() != null) {
            sysmlFactory = registry.getSysMLPackage().getEFactoryInstance();
        }
        if (registry.getKerMLPackage() != null) {
            kermlFactory = registry.getKerMLPackage().getEFactoryInstance();
        }
    }
    
    /**
     * 创建RequirementDefinition
     */
    public EObject createRequirement(String id, String name, String text) {
        EClass reqClass = registry.getEClass("RequirementDefinition");
        if (reqClass == null) {
            // 尝试其他可能的名称
            reqClass = registry.getEClass("Requirement");
            if (reqClass == null) {
                reqClass = registry.getEClass("RequirementUsage");
            }
        }
        
        if (reqClass == null) {
            throw new IllegalStateException("Cannot find RequirementDefinition class");
        }
        
        // 创建实例
        EObject requirement = EcoreUtil.create(reqClass);
        
        // 设置属性
        // SysML使用reqId而不是elementId
        if (!setAttributeValue(requirement, "reqId", id)) {
            setAttributeValue(requirement, "elementId", id);
        }
        // 使用declaredName而不是name
        if (!setAttributeValue(requirement, "declaredName", name)) {
            setAttributeValue(requirement, "name", name);
        }
        
        // 设置文本（可能在不同的属性中）
        if (!setAttributeValue(requirement, "text", text)) {
            if (!setAttributeValue(requirement, "doc", text)) {
                setAttributeValue(requirement, "documentation", text);
            }
        }
        
        return requirement;
    }
    
    /**
     * 创建PartDefinition
     */
    public EObject createPart(String id, String name, String type) {
        EClass partClass = registry.getEClass("PartDefinition");
        if (partClass == null) {
            partClass = registry.getEClass("PartUsage");
        }
        
        if (partClass == null) {
            throw new IllegalStateException("Cannot find PartDefinition class");
        }
        
        EObject part = EcoreUtil.create(partClass);
        setAttributeValue(part, "elementId", id);
        if (!setAttributeValue(part, "declaredName", name)) {
            setAttributeValue(part, "name", name);
        }
        
        return part;
    }
    
    /**
     * 创建PortDefinition
     */
    public EObject createPort(String id, String name, String direction, String type) {
        // 尝试不同的端口类名
        EClass portClass = registry.getEClass("PortDefinition");
        if (portClass == null) {
            portClass = registry.getEClass("PortUsage");
            if (portClass == null) {
                portClass = registry.getEClass("InterfaceDefinition");
            }
        }
        
        if (portClass == null) {
            throw new IllegalStateException("Cannot find Port-related class");
        }
        
        EObject port = EcoreUtil.create(portClass);
        setAttributeValue(port, "elementId", id);
        if (!setAttributeValue(port, "declaredName", name)) {
            setAttributeValue(port, "name", name);
        }
        
        // 设置方向（如果有这个属性）
        setAttributeValue(port, "direction", direction);
        
        return port;
    }
    
    /**
     * 创建ConnectionDefinition
     */
    public EObject createConnection(String id, String name, EObject source, EObject target) {
        // 尝试不同的连接类名
        EClass connClass = registry.getEClass("ConnectionDefinition");
        if (connClass == null) {
            connClass = registry.getEClass("ConnectionUsage");
            if (connClass == null) {
                connClass = registry.getEClass("ConnectorAsUsage");
            }
        }
        
        if (connClass == null) {
            throw new IllegalStateException("Cannot find Connection-related class");
        }
        
        EObject connection = EcoreUtil.create(connClass);
        setAttributeValue(connection, "elementId", id);
        if (!setAttributeValue(connection, "declaredName", name)) {
            setAttributeValue(connection, "name", name);
        }
        
        // 设置连接端点（需要根据实际元模型结构）
        // 这可能需要创建端点对象
        
        return connection;
    }
    
    /**
     * 批量创建元素
     */
    public EObject[] createBatch(ElementSpec[] specs) {
        List<EObject> elements = new ArrayList<>();
        
        for (ElementSpec spec : specs) {
            EObject element = null;
            
            switch (spec.getType()) {
                case "RequirementDefinition":
                    element = createRequirement(spec.getId(), spec.getName(), "");
                    break;
                case "PartDefinition":
                    element = createPart(spec.getId(), spec.getName(), "");
                    break;
                case "PortDefinition":
                    element = createPort(spec.getId(), spec.getName(), "inout", "data");
                    break;
                default:
                    // 尝试通用创建
                    EClass eClass = registry.getEClass(spec.getType());
                    if (eClass != null) {
                        element = EcoreUtil.create(eClass);
                        setAttributeValue(element, "elementId", spec.getId());
                        setAttributeValue(element, "name", spec.getName());
                    }
            }
            
            if (element != null) {
                elements.add(element);
            }
        }
        
        return elements.toArray(new EObject[0]);
    }
    
    /**
     * 获取属性值
     */
    public Object getAttributeValue(EObject obj, String attrName) {
        if (obj == null) return null;
        
        EClass eClass = obj.eClass();
        EAttribute attr = registry.getAttribute(eClass, attrName);
        
        if (attr != null && obj.eIsSet(attr)) {
            return obj.eGet(attr);
        }
        
        // 尝试通过所有属性查找
        for (EAttribute a : eClass.getEAllAttributes()) {
            if (a.getName().equalsIgnoreCase(attrName)) {
                return obj.eGet(a);
            }
        }
        
        return null;
    }
    
    /**
     * 设置属性值
     */
    private boolean setAttributeValue(EObject obj, String attrName, Object value) {
        if (obj == null) return false;
        
        EClass eClass = obj.eClass();
        
        // 先尝试精确匹配
        for (EAttribute attr : eClass.getEAllAttributes()) {
            if (attr.getName().equals(attrName)) {
                if (!attr.isChangeable() || attr.isDerived()) {
                    continue;
                }
                try {
                    obj.eSet(attr, value);
                    return true;
                } catch (Exception e) {
                    // 类型不匹配，继续尝试
                }
            }
        }
        
        // 尝试忽略大小写
        for (EAttribute attr : eClass.getEAllAttributes()) {
            if (attr.getName().equalsIgnoreCase(attrName)) {
                if (!attr.isChangeable() || attr.isDerived()) {
                    continue;
                }
                try {
                    obj.eSet(attr, value);
                    return true;
                } catch (Exception e) {
                    // 类型不匹配
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取需求文本
     */
    public String getRequirementText(EObject requirement) {
        // 尝试不同的属性名
        Object text = getAttributeValue(requirement, "text");
        if (text != null) return text.toString();
        
        text = getAttributeValue(requirement, "doc");
        if (text != null) return text.toString();
        
        text = getAttributeValue(requirement, "documentation");
        if (text != null) return text.toString();
        
        text = getAttributeValue(requirement, "body");
        if (text != null) return text.toString();
        
        return "";
    }
    
    /**
     * 获取端口方向
     */
    public String getPortDirection(EObject port) {
        Object dir = getAttributeValue(port, "direction");
        if (dir != null) return dir.toString();
        
        // 可能在其他属性中
        dir = getAttributeValue(port, "conjugated");
        if (dir != null && (Boolean)dir) {
            return "out";
        }
        
        return "inout";
    }
    
    /**
     * 获取连接端点
     */
    public EObject[] getConnectionEnds(EObject connection) {
        // 这需要根据实际的元模型结构实现
        // 可能在relatedElement或end引用中
        List<EObject> ends = new ArrayList<>();
        
        EClass connClass = connection.eClass();
        for (EReference ref : connClass.getEAllReferences()) {
            if (ref.getName().contains("end") || ref.getName().contains("End")) {
                Object value = connection.eGet(ref);
                if (value instanceof List) {
                    ends.addAll((List<EObject>) value);
                } else if (value instanceof EObject) {
                    ends.add((EObject) value);
                }
            }
        }
        
        return ends.toArray(new EObject[0]);
    }
    
    /**
     * 检查继承关系
     */
    public boolean isKindOf(EObject obj, String typeName) {
        if (obj == null) return false;
        return registry.isKindOf(obj.eClass(), typeName);
    }
}

/**
 * 元素规格
 */
class ElementSpec {
    private String type;
    private String id;
    private String name;
    
    public ElementSpec(String type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }
    
    public String getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}