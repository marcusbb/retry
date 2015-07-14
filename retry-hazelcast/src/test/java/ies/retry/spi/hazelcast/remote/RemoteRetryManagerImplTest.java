package ies.retry.spi.hazelcast.remote;

import static org.junit.Assert.*;
import ies.retry.xml.XMLRetryConfigMgr;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.Address;

public class RemoteRetryManagerImplTest {

	private HazelcastInstance remoteinstance;
	
	@Before
	public void before() {
		
		XMLRetryConfigMgr.setXML_FILE("remote/retry.xml");
		Config config = new ClasspathXmlConfig("hazelcast.xml");
		//config.setNetworkConfig(new NetworkConfig().setJoin(new Join().setTcpIpConfig(new TcpIpConfig().addMember("localhost"))));
		//config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		//TcpIpConfig tcpConfig = new TcpIpConfig();
		//tcpConfig.addMember("localhost");
		//config.getNetworkConfig().getJoin().setTcpIpConfig(tcpConfig);
		
		Hazelcast.newHazelcastInstance(config);
	}
	@After
	public void after() {
		XMLRetryConfigMgr.setXML_FILE(XMLRetryConfigMgr.DEFAULT_XML_FILE);
	}
	
	@Test
	public void testConfigInit() {
		RetryRemoteManagerImpl impl = new RetryRemoteManagerImpl();
		
	}

}
