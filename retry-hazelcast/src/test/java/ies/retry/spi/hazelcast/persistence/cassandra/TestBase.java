package ies.retry.spi.hazelcast.persistence.cassandra;

import java.io.IOException;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import driver.em.CUtils;
import driver.em.CassConfig;

public class TestBase {

	protected static CQLDataLoader loader = null;
	protected static Session session;
	
	@BeforeClass
	public static void beforeClass() throws ConfigurationException, TTransportException, IOException, InterruptedException {
		try {
			EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-unit.yaml");
		}catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		CassConfig config = new CassConfig();
		config.setContactHostsName(new String[]{"localhost"});
		config.setNativePort(9142);
		Cluster cluster = CUtils.createCluster(config);
		//cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet("olympia.cql",true, ks));
		loader = new CQLDataLoader(cluster.connect());
		loader.load(new ClassPathCQLDataSet("cassandra.cql",true, "icrs"));
		session = loader.getSession();
		//session = CUtils.createSession(cluster, ks);
		
		
	}
	
	@AfterClass
	public static void afterClass() {
		//TODO clean up data and tear down
		EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();

	}
}
