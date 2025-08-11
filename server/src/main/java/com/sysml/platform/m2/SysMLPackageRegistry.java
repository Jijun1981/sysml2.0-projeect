package com.sysml.platform.m2;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * SysML v2 包注册器
 * 实现RQ-M2-REG-001: EPackage注册
 * 负责加载和注册KerML/SysML的EPackage
 * 
 * @implements RQ-M2-REG-001
 */
@Component
public class SysMLPackageRegistry {
    
    private EPackage sysmlPackage;
    private EPackage kermlPackage;
    private final Map<String, EClass> classCache = new HashMap<>();
    private ResourceSet resourceSet;
    
    public SysMLPackageRegistry() {
        initializeResourceSet();
        // 自动尝试加载包
        loadDefaultPackages();
    }
    
    private void loadDefaultPackages() {
        try {
            // 加载真实的.ecore文件路径
            String kermlPath = "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/KerML.ecore";
            String sysmlPath = "/mnt/f/sysml2/opensource/sysml-v2-pilot/org.omg.sysml/model/SysML.ecore";
            
            loadKerMLPackage(kermlPath);
            loadAndRegister(sysmlPath);
        } catch (Exception e) {
            // 忽略加载错误，允许延迟加载
            System.out.println("Default packages not found, will load on demand: " + e.getMessage());
        }
    }
    
    /**
     * 检查包是否已加载
     */
    public boolean isLoaded() {
        return sysmlPackage != null || kermlPackage != null;
    }
    
    /**
     * 初始化资源集
     */
    private void initializeResourceSet() {
        resourceSet = new ResourceSetImpl();
        
        // 注册Ecore资源工厂
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("ecore", new EcoreResourceFactoryImpl());
        
        // 注册XMI资源工厂
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("xmi", new EcoreResourceFactoryImpl());
    }
    
    /**
     * 加载并注册SysML包
     */
    public boolean loadAndRegister(String ecorePath) {
        try {
            File ecoreFile = new File(ecorePath);
            if (!ecoreFile.exists()) {
                System.err.println("Ecore file not found: " + ecorePath);
                return false;
            }
            
            // 加载资源
            URI uri = URI.createFileURI(ecoreFile.getAbsolutePath());
            Resource resource = resourceSet.getResource(uri, true);
            
            if (resource.getContents().isEmpty()) {
                System.err.println("No content in ecore file: " + ecorePath);
                return false;
            }
            
            // 获取包
            sysmlPackage = (EPackage) resource.getContents().get(0);
            
            // 注册到全局注册表
            EPackage.Registry.INSTANCE.put(sysmlPackage.getNsURI(), sysmlPackage);
            
            // 缓存常用类
            cacheClasses(sysmlPackage);
            
            System.out.println("Successfully loaded SysML package: " + sysmlPackage.getName());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error loading SysML package: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 加载KerML包
     */
    public boolean loadKerMLPackage(String ecorePath) {
        try {
            File ecoreFile = new File(ecorePath);
            if (!ecoreFile.exists()) {
                System.err.println("KerML ecore file not found: " + ecorePath);
                return false;
            }
            
            URI uri = URI.createFileURI(ecoreFile.getAbsolutePath());
            Resource resource = resourceSet.getResource(uri, true);
            
            if (resource.getContents().isEmpty()) {
                return false;
            }
            
            kermlPackage = (EPackage) resource.getContents().get(0);
            EPackage.Registry.INSTANCE.put(kermlPackage.getNsURI(), kermlPackage);
            
            cacheClasses(kermlPackage);
            
            System.out.println("Successfully loaded KerML package: " + kermlPackage.getName());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error loading KerML package: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 缓存包中的类
     */
    private void cacheClasses(EPackage pkg) {
        for (EClassifier classifier : pkg.getEClassifiers()) {
            if (classifier instanceof EClass) {
                EClass eClass = (EClass) classifier;
                classCache.put(eClass.getName(), eClass);
            }
        }
        
        // 递归处理子包
        for (EPackage subPkg : pkg.getESubpackages()) {
            cacheClasses(subPkg);
        }
    }
    
    /**
     * 获取SysML包
     */
    public EPackage getSysMLPackage() {
        return sysmlPackage;
    }
    
    /**
     * 获取KerML包
     */
    public EPackage getKerMLPackage() {
        return kermlPackage;
    }
    
    /**
     * 根据名称获取EClass
     */
    public EClass getEClass(String className) {
        // 先从缓存查找
        EClass cached = classCache.get(className);
        if (cached != null) {
            return cached;
        }
        
        // 从SysML包查找
        if (sysmlPackage != null) {
            EClass found = findClassInPackage(sysmlPackage, className);
            if (found != null) {
                classCache.put(className, found);
                return found;
            }
        }
        
        // 从KerML包查找
        if (kermlPackage != null) {
            EClass found = findClassInPackage(kermlPackage, className);
            if (found != null) {
                classCache.put(className, found);
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * 获取KerML类
     */
    public EClass getKerMLClass(String className) {
        if (kermlPackage == null) {
            return null;
        }
        return findClassInPackage(kermlPackage, className);
    }
    
    /**
     * 在包中查找类
     */
    private EClass findClassInPackage(EPackage pkg, String className) {
        for (EClassifier classifier : pkg.getEClassifiers()) {
            if (classifier instanceof EClass && classifier.getName().equals(className)) {
                return (EClass) classifier;
            }
        }
        
        // 递归查找子包
        for (EPackage subPkg : pkg.getESubpackages()) {
            EClass found = findClassInPackage(subPkg, className);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * 获取属性
     */
    public EAttribute getAttribute(EClass eClass, String attrName) {
        if (eClass == null) {
            return null;
        }
        
        // 查找本类属性
        for (EAttribute attr : eClass.getEAttributes()) {
            if (attr.getName().equals(attrName)) {
                return attr;
            }
        }
        
        // 查找继承的属性
        for (EClass superType : eClass.getEAllSuperTypes()) {
            for (EAttribute attr : superType.getEAttributes()) {
                if (attr.getName().equals(attrName)) {
                    return attr;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查继承关系
     */
    public boolean isKindOf(EClass eClass, String superClassName) {
        if (eClass == null || superClassName == null) {
            return false;
        }
        
        // 检查自身
        if (eClass.getName().equals(superClassName)) {
            return true;
        }
        
        // 检查所有父类
        for (EClass superType : eClass.getEAllSuperTypes()) {
            if (superType.getName().equals(superClassName)) {
                return true;
            }
        }
        
        return false;
    }
}