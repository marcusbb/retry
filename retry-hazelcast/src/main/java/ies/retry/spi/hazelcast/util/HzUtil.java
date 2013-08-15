package ies.retry.spi.hazelcast.util;

import ies.retry.ConfigException;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.xml.XMLRetryConfigMgr;
import provision.services.logging.Logger;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HzUtil {

	static String HZ_CONFIG_FILE = HazelcastRetryImpl.HZ_CONFIG_FILE;
	static String CALLER = HzUtil.class.getName();
	
	public static HazelcastInstance loadHzConfiguration() {
		//XMLRetryConfigMgr xmlconfigMgr = (XMLRetryConfigMgr)configMgr;
		HazelcastInstance h1 = null;
		try {
			Config config = null;
			String dir = XMLRetryConfigMgr.getCONFIG_DIR();
			if (!"".equals(dir)) {
				String fileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_CONFIG_FILE;
				//config = new FileSystemXmlConfig(fileName);
				String xml = IOUtil.load(fileName);
				xml = StringUtil.replace(xml, System.getProperties());
				config = new InMemoryXmlConfig(xml); 
			}else {
				config = new ClasspathXmlConfig(HZ_CONFIG_FILE);
			}
			
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Loaded Hazelcast: " + config.toString());
			
								
			h1 = Hazelcast.newHazelcastInstance(config);
		}catch (Exception e) {
			Logger.warn(CALLER, "Load_Hazelcast_Configuration", "NO HAZELCAST CONFIGURATION FOUND: " + e.getMessage(), e);
			h1 = Hazelcast.getDefaultInstance();
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Using default config");
		}	
		return h1;
	}

	public static HazelcastInstance loadHzConfig() throws ConfigException {
		//XMLRetryConfigMgr xmlconfigMgr = (XMLRetryConfigMgr)configMgr;
		HazelcastInstance h1 = null;
		try {
			Config config = null;
			String dir = XMLRetryConfigMgr.getCONFIG_DIR();
			if (!"".equals(dir)) {
				String fileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_CONFIG_FILE;
				//config = new FileSystemXmlConfig(fileName);
				String xml = IOUtil.load(fileName);
				xml = StringUtil.replace(xml, System.getProperties());
				config = new InMemoryXmlConfig(xml); 
			}else {
				config = new ClasspathXmlConfig(HZ_CONFIG_FILE);
			}
			
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Loaded Hazelcast: " + config.toString());
			
								
			h1 = Hazelcast.newHazelcastInstance(config);
		} catch (ConfigException e) {
			throw e;
		}
		catch (Exception e) {
			Logger.warn(CALLER, "Load_Hazelcast_Configuration", "NO HAZELCAST CONFIGURATION FOUND: " + e.getMessage(), e);
			h1 = Hazelcast.getDefaultInstance();
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Using default config");
		}	
		return h1;
	}
}
