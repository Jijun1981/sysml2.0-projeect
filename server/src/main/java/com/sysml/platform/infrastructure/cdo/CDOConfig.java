package com.sysml.platform.infrastructure.cdo;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.IDBAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Lean CDO配置（dev: H2, prod: PostgreSQL） */
@Configuration
public class CDOConfig {

	// DataSource beans are defined in DataSourceConfig per profile

	@Bean
	@Profile("dev")
	public IRepository cdoRepositoryDev(DataSource dataSource) {
		Map<String, String> props = new HashMap<>();
		props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo");
		props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
		props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");

		IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
        IDBAdapter dbAdapter = getAdapter("org.eclipse.net4j.db.h2.H2Adapter", "h2");
        IDBStore store = CDODBUtil.createStore(
                mappingStrategy,
                dbAdapter,
                dbAdapter.createConnectionProvider(dataSource));
		return CDOServerUtil.createRepository("sysml", store, props);
	}

	@Bean
	@Profile("prod")
	public IRepository cdoRepositoryProd(DataSource dataSource) {
		Map<String, String> props = new HashMap<>();
		props.put(IRepository.Props.OVERRIDE_UUID, "sysml-repo");
		props.put(IRepository.Props.SUPPORTING_AUDITS, "false");
		props.put(IRepository.Props.SUPPORTING_BRANCHES, "false");

		IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(false);
        IDBAdapter dbAdapter = getAdapter("org.eclipse.net4j.db.postgresql.PostgreSQLAdapter", "postgresql");
        IDBStore store = CDODBUtil.createStore(
                mappingStrategy,
                dbAdapter,
                dbAdapter.createConnectionProvider(dataSource));
		return CDOServerUtil.createRepository("sysml", store, props);
	}

    private IDBAdapter getAdapter(String fqcn, String registryName) {
        try {
            Class<?> clazz = Class.forName(fqcn);
            return (IDBAdapter) clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable ignore) {
            return DBUtil.getDBAdapter(registryName);
        }
    }
}
