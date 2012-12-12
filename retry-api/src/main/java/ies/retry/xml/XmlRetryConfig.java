package ies.retry.xml;

import ies.retry.RetryConfiguration;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="retry")
public class XmlRetryConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7084739011201169886L;
	private String provider;
	private List<RetryConfiguration> typeConfig;
	
	
	
	public List<RetryConfiguration> getTypeConfig() {
		return typeConfig;
	}

	public void setTypeConfig(List<RetryConfiguration> typeConfig) {
		this.typeConfig = typeConfig;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	
	
	
}
