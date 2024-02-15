package fi.luomus.triplestore.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import fi.luomus.commons.db.connectivity.ConnectionDescription;

public class DataSourceDefinition {

	public static HikariDataSource initDataSource(ConnectionDescription desc) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(desc.url());
		config.setUsername(desc.username());
		config.setPassword(desc.password());
		config.setDriverClassName(desc.driver());

		config.setAutoCommit(true); // non-transaction mode: connection will be set to transaction mode if updates are done

		config.setConnectionTimeout(15000); // 15 seconds
		config.setMaximumPoolSize(80);
		config.setIdleTimeout(60000); // 1 minute
		config.setMaxLifetime(60000); // 1 minute -- the longest allowed query

		return new HikariDataSource(config);
	}

}

