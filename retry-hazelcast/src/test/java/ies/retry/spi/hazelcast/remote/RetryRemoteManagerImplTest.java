package ies.retry.spi.hazelcast.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hazelcast.client.HazelcastClient;

import ies.retry.Retry;
import ies.retry.RetryCallback;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.RetryManager;
import ies.retry.spi.hazelcast.CallbackManager;
import ies.retry.spi.hazelcast.CallbackRemoteProxy;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.xml.XMLRetryConfigMgr;

public class RetryRemoteManagerImplTest {

	static RetryManager server;
	
	@BeforeClass
	public static void beforeClass() {
		server = new HazelcastRetryImpl();
	}
	@AfterClass
	public static void afterClass() {
		//HzIntegrationTestUtil.afterClass();
		server.shutdown();
		Retry.setRetryManager(null);
		XMLRetryConfigMgr.setXML_FILE(XMLRetryConfigMgr.DEFAULT_XML_FILE);
	}
	//
	//@Test
	public void instantiateAndDestroy() {
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		
		RetryManager clientManager = Retry.getRetryManager();
		
		assertTrue(clientManager instanceof RetryRemoteManagerImpl);
		
		clientManager.shutdown();
		
	}
	
	@Test
	public void multiClient() {
		Retry.setRetryManager(server);
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RetryRemoteManagerImpl client1 = new RetryRemoteManagerImpl();
		RetryRemoteManagerImpl client2 = new RetryRemoteManagerImpl();
		
		assertNotNull(client1);
		assertNotNull(client2);
		
		client1.shutdown();
		client2.shutdown();
	}
	
	@Test
	public void configManagerHandle() {
		
		Retry.setRetryManager(server);
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RemoteConfigManager configManagerBefore = new RemoteConfigManager();
		HazelcastClient hzclient = HazelcastClient.newHazelcastClient(((RemoteXmlConfig)configManagerBefore.getConfig()).getHzClientConfig());
		configManagerBefore.setHzInstance(hzclient);
		
		//loaded types from original server retry_config.xml
		assertEquals(3,configManagerBefore.getConfigMap().size());
		
		//client adds one more configuration
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config_extra_type.xml");
		RetryRemoteManagerImpl client = new RetryRemoteManagerImpl();
		RetryConfigManager configManager = client.getConfigManager();
		assertTrue(configManager instanceof RemoteConfigManager);
		
		Map<String,RetryConfiguration> configList = configManager.getConfigMap();
		
		assertEquals(4,configList.size());
		
		HashSet<String> availSet = new HashSet<>();
		availSet.add("POKE");availSet.add("ARCHIVE_ON");availSet.add("ARCHIVE_OFF");availSet.add("ONCE_ONLY");
		for (RetryConfiguration config: configList.values()) {
			availSet.remove(config.getType());
		}
		assertEquals(0,availSet.size());
	}
	
	@Test
	public void addAndGet() {
		
		Retry.setRetryManager(server);
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RemoteConfigManager configManagerBefore = new RemoteConfigManager();
		HazelcastClient hzclient = HazelcastClient.newHazelcastClient(((RemoteXmlConfig)configManagerBefore.getConfig()).getHzClientConfig());
		configManagerBefore.setHzInstance(hzclient);
		
		//loaded types from original server retry_config.xml
		assertEquals(3,configManagerBefore.getConfigMap().size());
		
		//client adds one more configuration
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config_extra_type.xml");
		RetryRemoteManagerImpl client = new RetryRemoteManagerImpl();
		RetryConfigManager configManager = client.getConfigManager();
		assertTrue(configManager instanceof RemoteConfigManager);
		
		Map<String,RetryConfiguration> configList = configManager.getConfigMap();
		
		assertEquals(4,configList.size());
		
		HashSet<String> availSet = new HashSet<>();
		availSet.add("POKE");availSet.add("ARCHIVE_ON");availSet.add("ARCHIVE_OFF");availSet.add("ONCE_ONLY");
		for (RetryConfiguration config: configList.values()) {
			availSet.remove(config.getType());
		}
		assertEquals(0,availSet.size());
	}
	
	@Test
	public void register() throws InterruptedException {
		Retry.setRetryManager(server);
		HzUtil.HZ_CONFIG_FILE = "remote/client-cluster.xml";
		HzUtil.buildHzInstanceWith("retry_client_cluster");
		
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RetryRemoteManagerImpl client = new RetryRemoteManagerImpl();
		
				
		assertTrue(server.registeredCallback("POKE") instanceof CallbackRemoteProxy);
		
		CallbackManager cbm = ((HazelcastRetryImpl)server).getCallbackManager();
		cbm.tryDequeue("POKE");
				
		client.shutdown();
		HzUtil.HZ_CONFIG_FILE = "hazelcast.xml";
		
	}
	@Test
	public void endToend() throws InterruptedException {
		Retry.setRetryManager(server);
		HzUtil.HZ_CONFIG_FILE = "remote/client-cluster.xml";
		HzUtil.buildHzInstanceWith("retry_client_cluster");
		
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RetryRemoteManagerImpl client = new RetryRemoteManagerImpl();
		
		final CountDownLatch latch = new CountDownLatch(1);
		client.registerCallback(new RetryCallback() {
			
			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				latch.countDown();
				return true;
			}
		}, "POKE");
		
		//add retry
		client.addRetry(new RetryHolder("test-id1", "POKE", new HashMap<>()));
		
		CallbackManager cbm = ((HazelcastRetryImpl)server).getCallbackManager();
		cbm.tryDequeue("POKE");
		
		assertTrue(latch.await(100, TimeUnit.SECONDS));
		
		client.shutdown();
		HzUtil.HZ_CONFIG_FILE = "hazelcast.xml";
		
	}

}
