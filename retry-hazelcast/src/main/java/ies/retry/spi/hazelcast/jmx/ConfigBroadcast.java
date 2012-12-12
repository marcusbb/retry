package ies.retry.spi.hazelcast.jmx;

import java.io.Serializable;

import ies.retry.xml.XmlRetryConfig;

public class ConfigBroadcast implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -780446256232518051L;
	private XmlRetryConfig xmlConfig;

	public ConfigBroadcast() {}
	public ConfigBroadcast(XmlRetryConfig config) {
		this.xmlConfig = config;
	}
	public XmlRetryConfig getXmlConfig() {
		return xmlConfig;
	}

	public void setXmlConfig(XmlRetryConfig xmlConfig) {
		this.xmlConfig = xmlConfig;
	}
	
	
	
}
