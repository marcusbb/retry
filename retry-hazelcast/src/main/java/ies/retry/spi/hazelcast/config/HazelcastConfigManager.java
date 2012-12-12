package ies.retry.spi.hazelcast.config;

import java.util.Map;

import ies.retry.RetryConfiguration;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;

public class HazelcastConfigManager extends XMLRetryConfigMgr {

	HazelcastRetryImpl retryImpl;
	
	public HazelcastConfigManager(HazelcastRetryImpl retryImpl) {
		this.retryImpl = retryImpl;
	}
	
	@Override
	public void addConfiguration(RetryConfiguration newConfig) {
		//This can be moved into the api
		//super.addConfiguration(config);
		Map<String,RetryConfiguration> configMap = getConfigMap();
		XmlRetryConfig config = super.getConfig();
		configMap.put(newConfig.getType(), newConfig);
		int i = 0;
		boolean found = false;
		for (RetryConfiguration existConfig:config.getTypeConfig()) {
			if (existConfig.getType().equals(newConfig.getType())) {
				found = true;break;
			}
			i++;
		}
		if (found)
			config.getTypeConfig().set(i, newConfig);
		else
			config.getTypeConfig().add(newConfig);
		
		//this is the hazelcast operation specific
		retryImpl.initStat(newConfig);
		retryImpl.getStateMgr().init(newConfig);
	}	
	
	public HazelcastXmlConfig getHzConfig() {
		return (HazelcastXmlConfig)getConfig();
	}
	
	
}
