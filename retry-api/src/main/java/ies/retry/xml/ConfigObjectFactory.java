package ies.retry.xml;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ConfigObjectFactory {

	public XmlRetryConfig createRetryConfiguration() {
		return new XmlRetryConfig();
	}
}
