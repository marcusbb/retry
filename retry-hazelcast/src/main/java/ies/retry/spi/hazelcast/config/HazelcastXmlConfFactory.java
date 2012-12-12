package ies.retry.spi.hazelcast.config;

import ies.retry.xml.ConfigObjectFactory;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class HazelcastXmlConfFactory extends ConfigObjectFactory {

	@Override
	public HazelcastXmlConfig createRetryConfiguration() {
		return new HazelcastXmlConfig();
	}
}
