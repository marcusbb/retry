package ies.retry.spi.hazelcast;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.hazelcast.core.HazelcastInstance;

import ies.retry.Retry;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;



public abstract class HzIntegrationTestUtil {


	public static String ORIG_RETRY_XML = "retry_config.xml";
	public static String ORIG_HZ_XML = "hazelcast.xml";
	public static void beforeClass() {
		/*HazelcastInstance inst = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		if (inst!= null) {
			inst.getLifecycleService().shutdown();
			((HazelcastRetryImpl)Retry.getRetryManager()).setH1(null);
		}*/
		//emf = PersistenceUtil.getEMFactory("retryPool");
		try {
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		        Connection con = DriverManager.getConnection("jdbc:derby:memory:testDB;create=true");
		        
		        if (!tableExists(con, "retries"))
		        	executeResource(con, "derby.sql");
		        if (!tableExists(con, "retries_archive"))
		        	executeResource(con, "derby2.sql");
		        con.close();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		XMLRetryConfigMgr.setXML_FILE( XMLRetryConfigMgr.DEFAULT_XML_FILE);
		HzUtil.HZ_CONFIG_FILE = "hazelcast.xml";
	}
	
	private static void executeResource(Connection con, String resource) throws IOException,SQLException {
		InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        byte []b = new byte[ins.available()];
        ins.read(b);
        con.createStatement().execute(new String(b));
	}
	private static boolean tableExists(Connection con, String table) throws IOException,SQLException {
		try {
			con.createStatement().execute("select * from " + table);
			return true;
		}catch (SQLException e) {
			return false;
		}
	}
	public static void afterClass()  {
		HazelcastInstance inst = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		if (inst != null &&
			inst.getLifecycleService().isRunning() ) {
			inst.getLifecycleService().shutdown();
		}
		((HazelcastRetryImpl)Retry.getRetryManager()).setH1(null);
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(ORIG_RETRY_XML);
		HzUtil.HZ_CONFIG_FILE = ORIG_HZ_XML;
		
	}
}
