package com.sysml.platform.m2;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 适配器 - 提供SysML v2建模功能
 * 使用EMF动态模型API
 * 
 * @implements EP-M2
 */
@Component
public class SysMLModelAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(SysMLModelAdapter.class);
    
    private EPackage sysmlPackage;
    private EFactory sysmlFactory;
    private Map<String, EClass> classCache = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing SysML Model Adapter");
        createSysMLMetamodel();
        logger.info("SysML Metamodel created successfully");
    }
    
    /**
     * 创建简化的SysML元模型
     */
    private void createSysMLMetamodel() {
        // 创建包
        sysmlPackage = EcoreFactory.eINSTANCE.createEPackage();
        sysmlPackage.setName("sysml");
        sysmlPackage.setNsPrefix("sysml");
        sysmlPackage.setNsURI("http://www.omg.org/sysml/v2");
        
        // 创建工厂
        sysmlFactory = sysmlPackage.getEFactoryInstance();
        
        // 创建基础类
        EClass elementClass = createEClass("Element");
        addAttribute(elementClass, "name", EcorePackage.Literals.ESTRING);
        addAttribute(elementClass, "id", EcorePackage.Literals.ESTRING);
        
        // 创建RequirementDefinition
        EClass requirementClass = createEClass("RequirementDefinition");
        requirementClass.getESuperTypes().add(elementClass);
        addAttribute(requirementClass, "text", EcorePackage.Literals.ESTRING);
        addAttribute(requirementClass, "priority", EcorePackage.Literals.ESTRING);
        addAttribute(requirementClass, "status", EcorePackage.Literals.ESTRING);
        addAttribute(requirementClass, "kind", EcorePackage.Literals.ESTRING);
        
        // 创建PartDefinition (Block)
        EClass partClass = createEClass("PartDefinition");
        partClass.getESuperTypes().add(elementClass);
        
        // 创建InterfaceDefinition
        EClass interfaceClass = createEClass("InterfaceDefinition");
        interfaceClass.getESuperTypes().add(elementClass);
        
        // 创建PortDefinition
        EClass portClass = createEClass("PortDefinition");
        portClass.getESuperTypes().add(elementClass);
        
        // 创建关系类
        EClass relationshipClass = createEClass("Relationship");
        relationshipClass.getESuperTypes().add(elementClass);
        addReference(relationshipClass, "source", elementClass, false);
        addReference(relationshipClass, "target", elementClass, false);
        
        // 创建Subsetting (派生关系)
        EClass subsettingClass = createEClass("Subsetting");
        subsettingClass.getESuperTypes().add(relationshipClass);
        
        // 创建Redefinition (细化关系)
        EClass redefinitionClass = createEClass("Redefinition");
        redefinitionClass.getESuperTypes().add(relationshipClass);
        
        // 创建SatisfyRelation (满足关系)
        EClass satisfyClass = createEClass("SatisfyRelation");
        satisfyClass.getESuperTypes().add(relationshipClass);
        
        // 注册到全局Registry
        EPackage.Registry.INSTANCE.put(sysmlPackage.getNsURI(), sysmlPackage);
    }
    
    private EClass createEClass(String name) {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(name);
        sysmlPackage.getEClassifiers().add(eClass);
        classCache.put(name, eClass);
        return eClass;
    }
    
    private void addAttribute(EClass eClass, String name, EDataType type) {
        EAttribute attr = EcoreFactory.eINSTANCE.createEAttribute();
        attr.setName(name);
        attr.setEType(type);
        eClass.getEStructuralFeatures().add(attr);
    }
    
    private void addReference(EClass eClass, String name, EClass refType, boolean containment) {
        EReference ref = EcoreFactory.eINSTANCE.createEReference();
        ref.setName(name);
        ref.setEType(refType);
        ref.setContainment(containment);
        eClass.getEStructuralFeatures().add(ref);
    }
    
    /**
     * 创建需求定义
     */
    public EObject createRequirementDefinition(String id, String name, String text) {
        EClass reqClass = classCache.get("RequirementDefinition");
        EObject req = sysmlFactory.create(reqClass);
        req.eSet(reqClass.getEStructuralFeature("id"), id);
        req.eSet(reqClass.getEStructuralFeature("name"), name);
        req.eSet(reqClass.getEStructuralFeature("text"), text);
        return req;
    }
    
    /**
     * 创建Part定义
     */
    public EObject createPartDefinition(String id, String name) {
        EClass partClass = classCache.get("PartDefinition");
        EObject part = sysmlFactory.create(partClass);
        part.eSet(partClass.getEStructuralFeature("id"), id);
        part.eSet(partClass.getEStructuralFeature("name"), name);
        return part;
    }
    
    /**
     * 创建Interface定义
     */
    public EObject createInterfaceDefinition(String id, String name) {
        EClass ifaceClass = classCache.get("InterfaceDefinition");
        EObject iface = sysmlFactory.create(ifaceClass);
        iface.eSet(ifaceClass.getEStructuralFeature("id"), id);
        iface.eSet(ifaceClass.getEStructuralFeature("name"), name);
        return iface;
    }
    
    /**
     * 创建派生关系
     */
    public EObject createDeriveRelation(EObject source, EObject target) {
        EClass subClass = classCache.get("Subsetting");
        EObject subsetting = sysmlFactory.create(subClass);
        subsetting.eSet(subClass.getEStructuralFeature("source"), source);
        subsetting.eSet(subClass.getEStructuralFeature("target"), target);
        subsetting.eSet(subClass.getEStructuralFeature("name"), "derives");
        return subsetting;
    }
    
    /**
     * 创建细化关系
     */
    public EObject createRefineRelation(EObject source, EObject target) {
        EClass refClass = classCache.get("Redefinition");
        EObject redefinition = sysmlFactory.create(refClass);
        redefinition.eSet(refClass.getEStructuralFeature("source"), source);
        redefinition.eSet(refClass.getEStructuralFeature("target"), target);
        redefinition.eSet(refClass.getEStructuralFeature("name"), "refines");
        return redefinition;
    }
    
    /**
     * 创建满足关系
     */
    public EObject createSatisfyRelation(EObject requirement, EObject element) {
        EClass satClass = classCache.get("SatisfyRelation");
        EObject satisfy = sysmlFactory.create(satClass);
        satisfy.eSet(satClass.getEStructuralFeature("source"), element);
        satisfy.eSet(satClass.getEStructuralFeature("target"), requirement);
        satisfy.eSet(satClass.getEStructuralFeature("name"), "satisfies");
        return satisfy;
    }
    
    public EPackage getPackage() {
        return sysmlPackage;
    }
    
    public EFactory getFactory() {
        return sysmlFactory;
    }
    
    public EClass getEClass(String name) {
        return classCache.get(name);
    }
    
    public boolean isMetamodelLoaded() {
        return sysmlPackage != null && sysmlFactory != null;
    }
}