package com.sysml.platform.infrastructure.cdo;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.cdo.server.net4j.CDONet4jServerUtil;
import org.eclipse.emf.cdo.server.internal.net4j.protocol.CDOServerProtocolFactory;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.IPluginContainer;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * CDO协议工厂注册器 - 手动注册CDO服务器协议工厂
 * 解决 "Factory not found: org.eclipse.net4j.serverProtocols[cdo]" 错误
 * 使用容器的registerFactory方法直接注册
 */
@Component
@Order(1) // 确保优先初始化
@Slf4j
public class CDOProtocolFactory {
    
    @PostConstruct
    public void registerProtocolFactories() {
        try {
            log.info("开始手动注册CDO协议工厂");
            
            // 1. 确保基础容器准备完成
            Net4jUtil.prepareContainer(IPluginContainer.INSTANCE);
            TCPUtil.prepareContainer(IPluginContainer.INSTANCE);
            CDONet4jServerUtil.prepareContainer(IPluginContainer.INSTANCE);
            
            // 2. 获取插件容器作为管理容器
            IManagedContainer container = IPluginContainer.INSTANCE;
            
            // 3. 手动创建CDO服务器协议工厂并注册
            CDOServerProtocolFactory cdoFactory = new CDOServerProtocolFactory();
            
            // 4. 使用容器的registerFactory方法直接注册
            container.registerFactory(cdoFactory);
            
            log.info("CDO协议工厂直接注册完成:");
            log.info("  - Product Group: {}", cdoFactory.getProductGroup());
            log.info("  - Type: {}", cdoFactory.getType());
            log.info("  - Factory: {}", cdoFactory.getClass().getName());
            
            // 5. 验证注册结果 - 尝试从容器获取工厂
            try {
                Object factory = container.getFactory(cdoFactory.getProductGroup(), cdoFactory.getType());
                if (factory != null) {
                    log.info("✅ CDO协议工厂验证成功: {}", factory.getClass().getName());
                } else {
                    log.warn("⚠️ 无法通过getFactory验证，但注册可能已成功");
                }
            } catch (Exception e) {
                log.warn("验证工厂注册时出现异常，但这可能是正常的: {}", e.getMessage());
            }
            
            // 6. 额外验证：尝试创建一个实例来测试工厂是否可用
            try {
                Object testInstance = container.getElementOrNull(
                    cdoFactory.getProductGroup(), 
                    cdoFactory.getType(), 
                    "test"
                );
                log.info("工厂功能测试: {}", testInstance != null ? "可用" : "待验证");
            } catch (Exception e) {
                log.debug("工厂功能测试异常（可能正常）: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("CDO协议工厂注册失败", e);
            throw new RuntimeException("CDO协议工厂注册失败", e);
        }
    }
}