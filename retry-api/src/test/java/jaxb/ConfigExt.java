package jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import ies.retry.xml.XmlRetryConfig;

@XmlRootElement(name="retry")
public class ConfigExt extends XmlRetryConfig {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1406412916599928976L;

	//@XmlElement
	private String strProperty;

	private OtherConfig otherConfig;
	
	public String getStrProperty() {
		return strProperty;
	}

	public void setStrProperty(String strProperty) {
		this.strProperty = strProperty;
	}

	public OtherConfig getOtherConfig() {
		return otherConfig;
	}

	public void setOtherConfig(OtherConfig otherConfig) {
		this.otherConfig = otherConfig;
	}
	
	
	
}
