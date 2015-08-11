package ies.retry.spi.hazelcast.remote;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlTransient;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.config.GroupConfig;

import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.xml.ConfigObjectFactory;
import ies.retry.xml.XmlRetryConfig;

/**
 * 
 * Some duplication of {@link ClientConfig}, where we need to 
 * make XML serializable.
 * 
 */
public class RemoteXmlConfig extends XmlRetryConfig {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3844414914400811803L;

	@XmlRegistry
	public static class XmlConfFactory extends ConfigObjectFactory {

		@Override
		public RemoteXmlConfig createRetryConfiguration() {
			return new RemoteXmlConfig();
		}
	}
	
	//Use String as they are serializable
	
	private List<String> addressList = new ArrayList<String>(10);
	
	

	private GroupConfig groupConfig = new GroupConfig();
	
	public RemoteXmlConfig() {}
	
	public RemoteXmlConfig(List<String> addressList) {
		this.addressList = addressList;
	}
	
	public RemoteXmlConfig(List<String> addressList, GroupConfig groupConfig) {
		this.addressList = addressList;
		this.groupConfig = groupConfig;
	}
	
	public GroupConfig getGroupConfig() {
		return groupConfig;
	}
	public void setGroupConfig(GroupConfig groupConfig) {
		this.groupConfig = groupConfig;
	}
	@XmlElement( name="address" )
	@XmlElementWrapper( name="address-list" )
	public List<String> getAddressList() {
		return addressList;
	}
	public void setAddressList(List<String> addressList) {
		this.addressList = addressList;
	}
	
	@XmlTransient
	public ClientConfig getHzClientConfig() {
		ClientConfig cc = new ClientConfig();
		
		cc.addAddress(addressList.toArray(new String[]{}));
		
		cc.setGroupConfig(groupConfig);
		
		return cc;
	}
	
}
