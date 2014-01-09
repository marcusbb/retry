package ies.retry.spi.hazelcast.config;

import ies.retry.RetryConfiguration;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hazelcast.config.Config;

public class HazelcastConfigManager extends XMLRetryConfigMgr {

	HazelcastRetryImpl retryImpl;
	
	Set<ConfigListener> listenerSet;
	
	//Actual HZ configuration
	private Config hzConfiguration;
	
	public HazelcastConfigManager(HazelcastRetryImpl retryImpl) {
		this.retryImpl = retryImpl;
		this.listenerSet = new HashSet<ConfigListener>();
		
		//this.hzConfiguration = retryImpl.getH1().getConfig();
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
		retryImpl.getCallbackManager().init(newConfig.getType());
	}	
	
	public HazelcastXmlConfig getRetryHzConfig() {
		return (HazelcastXmlConfig)getConfig();
	}
	
	public Config getHzConfiguration() {
		if (retryImpl.getH1() != null) {
			return retryImpl.getH1().getConfig();
		}
		throw new IllegalStateException("Retry Service is hasn't started hazelcast");
	}
	public void addListener(ConfigListener listener) {
		listenerSet.add(listener);
	}
	public void removeListener(ConfigListener listener) {
		listenerSet.remove(listener);
	}
	public void notifyListeners() {
		for (ConfigListener listener:listenerSet) {
			listener.onConfigChange(getRetryHzConfig());
		}
	}
	
	
}
