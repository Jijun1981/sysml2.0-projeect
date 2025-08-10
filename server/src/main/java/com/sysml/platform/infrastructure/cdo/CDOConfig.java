package com.sysml.platform.infrastructure.cdo;

import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Lean CDO配置
 * - 单Repository
 * - 无分支
 * - 无审计
 * - 无锁
 */
@Configuration
public class CDOConfig {
    
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:./data/cdo;AUTO_SERVER=TRUE");
        return dataSource;
    }
    
    @Bean
    public IRepository cdoRepository(DataSource dataSource) {
        Map<String, String> props = new HashMap<>();
        props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo");
        props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
        props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");
        
        IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
        IDBStore store = CDODBUtil.createStore(
            mappingStrategy,
            CDODBUtil.createDBAdapter("h2"),
            CDODBUtil.createConnectionProvider(dataSource)
        );
        
        return CDOServerUtil.createRepository("sysml", store, props);
    }
}