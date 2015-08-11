package ies.retry.spi.hazelcast.remote;

import static org.junit.Assert.*;
import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;

import org.junit.Test;

public class RemoteXmlConfigTest {

	@Test
	public void testParse() {
		XMLRetryConfigMgr.setXML_FILE("remote/retry_config.xml");
		RemoteConfigManager configManager = new RemoteConfigManager();
		XmlRetryConfig config = configManager.getConfig();
		
		assertNotNull(config);
		
		assertEquals(1,config.getTypeConfig().size());
		
		
	}

}
