package fi.luomus.triplestore.dao;

import fi.luomus.commons.db.connectivity.ConnectionDescription;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public class DataSourceDefinition {

	public static DataSource initDataSource(ConnectionDescription desc) {
		PoolProperties p = new PoolProperties();
		p.setUrl(desc.url());
		p.setDriverClassName(desc.driver());
		p.setUsername(desc.username());
		p.setPassword(desc.password());
		
		p.setDefaultAutoCommit(true); // non-transaction mode: connection will be set to transaction mode if updates are done
		p.setMaxActive(10);
		p.setMaxIdle(8);
		p.setMinIdle(2);
		p.setInitialSize(2);
		p.setRemoveAbandonedTimeout(5*60); // 5 minutes -- the longest possible query
		p.setTestOnBorrow(true);
		p.setValidationQuery("SELECT 1 FROM DUAL");
		p.setValidationInterval(60 * 1000); // 60 seconds
		p.setJdbcInterceptors(
				"org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
				"org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
		
		DataSource datasource = new DataSource();
		datasource.setPoolProperties(p);
		return datasource;
	}

}

