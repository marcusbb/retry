package ies.retry.spi.hazelcast.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import ies.retry.xml.XMLRetryConfigMgr;
import provision.services.logging.Logger;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HzUtil {

	public static String HZ_CONFIG_FILE = "hazelcast.xml";
	public static String HZ_PROP_FILE = "hz.properties";
	
	
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
				//load in the system parameters:
				String propFileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_PROP_FILE;
				File propFile = new File(propFileName);
				if (propFile.exists()) {
					FileInputStream propFin = new FileInputStream(propFile);
					Properties properties = new Properties();
					properties.load(propFin);
					for (Object key:properties.keySet()) {
						Logger.info(CALLER, "Load_Hz_Prop","load","key",key,"value",properties.get(key));
						System.setProperty((String)key, (String)properties.get(key) );
					}
				}else {
					Logger.info(CALLER, "Load_Hz_Prop","No Properties to set");
				}
				
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

	
}
