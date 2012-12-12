package jaxb;

import ies.retry.xml.ConfigObjectFactory;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ConfigExtObjFactory extends ConfigObjectFactory {

	public ConfigExt createRetryConfiguration() {
		return new ConfigExt();
	}
}
