package ies.retry.spi.hazelcast.remote;

import ies.retry.xml.ConfigObjectFactory;
import ies.retry.xml.XMLRetryConfigMgr;

import javax.xml.bind.annotation.XmlRegistry;


public class ClientConfigManager extends XMLRetryConfigMgr {

	
	public ClientConfigManager() {
		
		setFactory(new Factory());
		setJaxbConfigClass(HzClientXmlConfig.class);
		
	}
	
	
	@XmlRegistry
	public static class Factory extends ConfigObjectFactory {
		
		@Override
		public HzClientXmlConfig createRetryConfiguration() {
			return new HzClientXmlConfig();
		}
	}


	public HzClientXmlConfig getConfig() {
		return (HzClientXmlConfig)super.getConfig();
	}
	public void setConfig(HzClientXmlConfig config) {
		super.setConfig(config);
	}
	

}
