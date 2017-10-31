package fi.luomus.triplestore.dao;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import fi.luomus.commons.db.connectivity.ConnectionDescription;

public class DataSourceDefinition {

	public static DataSource initDataSource(ConnectionDescription desc) {
		PoolProperties p = new PoolProperties();
		p.setUrl(desc.url());
		p.setDriverClassName(desc.driver());
		p.setUsername(desc.username());
		p.setPassword(desc.password());
		
		p.setDefaultAutoCommit(true); // non-transaction mode: connection will be set to transaction mode if updates are done
		p.setMaxActive(80);
		p.setMaxWait(15 * 1000); // 15 sec to wait for free connection before failing
		p.setMaxIdle(8);
		p.setMinIdle(2);
		p.setInitialSize(2);
		p.setRemoveAbandonedTimeout(60); // 60 sec -- the longest allowed query
		p.setAbandonWhenPercentageFull(50); // only abandon if 50% of connections are in use
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

