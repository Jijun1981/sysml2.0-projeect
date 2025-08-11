package com.sysml.platform.m2;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * SysML序列化器
 * 实现RQ-M2-ROUNDTRIP-003: 往返等价
 * 负责XMI和JSON格式的序列化/反序列化
 * 
 * @implements RQ-M2-ROUNDTRIP-003
 */
@Component
public class SysMLSerializer {
    
    private ResourceSet resourceSet;
    private SysMLPackageRegistry registry;
    private ObjectMapper jsonMapper;
    
    public SysMLSerializer() {
        this.jsonMapper = new ObjectMapper();
        initializeResourceSet();
    }
    
    /**
     * 初始化
     */
    public void initialize(SysMLPackageRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * 初始化资源集
     */
    private void initializeResourceSet() {
        resourceSet = new ResourceSetImpl();
        
        // 注册XMI工厂
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("xmi", new XMIResourceFactoryImpl());
        
        // 注册Ecore工厂
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("ecore", new EcoreResourceFactoryImpl());
        
        // 注册通配符
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    }
    
    /**
     * 保存到XMI文件
     */
    public boolean saveToXMI(EObject object, File file) {
        try {
            // 创建资源
            URI uri = URI.createFileURI(file.getAbsolutePath());
            Resource resource = resourceSet.createResource(uri);
            
            // 添加对象
            resource.getContents().add(object);
            
            // 保存选项
            Map<Object, Object> options = new HashMap<>();
            options.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
            
            // 保存
            resource.save(options);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving to XMI: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从XMI文件加载
     */
    public EObject loadFromXMI(File file) {
        try {
            // 加载资源
            URI uri = URI.createFileURI(file.getAbsolutePath());
            Resource resource = resourceSet.getResource(uri, true);
            
            // 返回第一个根对象
            if (!resource.getContents().isEmpty()) {
                return resource.getContents().get(0);
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error loading from XMI: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 转换为JSON
     */
    public String toJSON(EObject object) {
        try {
            ObjectNode jsonNode = jsonMapper.createObjectNode();
            
            // 添加类型信息
            EClass eClass = object.eClass();
            jsonNode.put("eClass", eClass.getName());
            jsonNode.put("nsURI", eClass.getEPackage().getNsURI());
            
            // 添加属性
            ObjectNode attributes = jsonMapper.createObjectNode();
            for (EAttribute attr : eClass.getEAllAttributes()) {
                if (object.eIsSet(attr)) {
                    Object value = object.eGet(attr);
                    if (value != null) {
                        if (value instanceof List) {
                            ArrayNode array = jsonMapper.createArrayNode();
                            for (Object item : (List<?>) value) {
                                array.add(item.toString());
                            }
                            attributes.set(attr.getName(), array);
                        } else {
                            attributes.put(attr.getName(), value.toString());
                        }
                    }
                }
            }
            jsonNode.set("attributes", attributes);
            
            // 添加引用（简化版，只记录ID）
            ObjectNode references = jsonMapper.createObjectNode();
            for (EReference ref : eClass.getEAllReferences()) {
                if (object.eIsSet(ref) && !ref.isDerived() && !ref.isTransient()) {
                    Object value = object.eGet(ref);
                    if (value instanceof EObject) {
                        EObject refObj = (EObject) value;
                        references.put(ref.getName(), EcoreUtil.getID(refObj));
                    } else if (value instanceof List) {
                        ArrayNode array = jsonMapper.createArrayNode();
                        for (Object item : (List<?>) value) {
                            if (item instanceof EObject) {
                                String id = EcoreUtil.getID((EObject) item);
                                if (id != null) {
                                    array.add(id);
                                }
                            }
                        }
                        if (array.size() > 0) {
                            references.set(ref.getName(), array);
                        }
                    }
                }
            }
            if (references.size() > 0) {
                jsonNode.set("references", references);
            }
            
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            
        } catch (Exception e) {
            System.err.println("Error converting to JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从JSON加载
     */
    public EObject fromJSON(String json, EClass expectedClass) {
        try {
            ObjectNode jsonNode = (ObjectNode) jsonMapper.readTree(json);
            
            // 获取类信息
            String className = jsonNode.get("eClass").asText();
            
            // 创建实例
            EObject object = EcoreUtil.create(expectedClass);
            
            // 设置属性
            ObjectNode attributes = (ObjectNode) jsonNode.get("attributes");
            if (attributes != null) {
                attributes.fields().forEachRemaining(entry -> {
                    String attrName = entry.getKey();
                    EAttribute attr = (EAttribute) expectedClass.getEStructuralFeature(attrName);
                    if (attr != null && attr.isChangeable() && !attr.isDerived()) {
                        try {
                            if (entry.getValue().isArray()) {
                                // 处理列表属性
                                List<Object> list = new ArrayList<>();
                                entry.getValue().forEach(node -> list.add(node.asText()));
                                object.eSet(attr, list);
                            } else {
                                // 处理单值属性
                                String value = entry.getValue().asText();
                                object.eSet(attr, EcoreUtil.createFromString(attr.getEAttributeType(), value));
                            }
                        } catch (Exception e) {
                            // 忽略设置失败的属性
                        }
                    }
                });
            }
            
            // 暂不处理引用（需要完整的对象图）
            
            return object;
            
        } catch (Exception e) {
            System.err.println("Error loading from JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 创建包含多个元素的包
     */
    public EObject createPackage(String name, EObject[] elements) {
        // 使用SysML的Package类
        EClass pkgClass = null;
        if (registry != null) {
            pkgClass = registry.getEClass("Package");
            if (pkgClass == null) {
                pkgClass = registry.getEClass("Namespace");
            }
        }
        
        if (pkgClass == null) {
            // 创建一个简单容器
            return elements.length > 0 ? elements[0] : null;
        }
        
        EObject pkg = EcoreUtil.create(pkgClass);
        
        // 设置名称
        EAttribute nameAttr = (EAttribute) pkgClass.getEStructuralFeature("name");
        if (nameAttr == null) {
            nameAttr = (EAttribute) pkgClass.getEStructuralFeature("declaredName");
        }
        if (nameAttr != null) {
            pkg.eSet(nameAttr, name);
        }
        
        // 添加元素（需要找到正确的containment引用）
        for (EReference ref : pkgClass.getEAllReferences()) {
            if (ref.isContainment() && ref.isMany()) {
                @SuppressWarnings("unchecked")
                List<EObject> contents = (List<EObject>) pkg.eGet(ref);
                contents.addAll(Arrays.asList(elements));
                break;
            }
        }
        
        return pkg;
    }
    
    /**
     * 获取包中的元素
     */
    public List<EObject> getPackageElements(EObject pkg) {
        List<EObject> elements = new ArrayList<>();
        
        if (pkg == null) {
            return elements;
        }
        
        // 查找containment引用
        for (EReference ref : pkg.eClass().getEAllReferences()) {
            if (ref.isContainment()) {
                Object value = pkg.eGet(ref);
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<EObject> list = (List<EObject>) value;
                    elements.addAll(list);
                } else if (value instanceof EObject) {
                    elements.add((EObject) value);
                }
            }
        }
        
        return elements;
    }
}