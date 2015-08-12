package ies.retry.spi.hazelcast.remote;

import java.net.InetSocketAddress;
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
	 * Convenience for non-jaxb serializable {@link ClientConfig} 
	 *
	 */
	public static class SeralizableClientConfig {
		
		@XmlTransient
		private ClientConfig cc;
		
		public GroupConfig groupConfig = new GroupConfig();
		
		@XmlElement( name="host" )
		@XmlElementWrapper( name="hosts" )
		public List<String> hostList = new ArrayList<String>(10);
		
		public int port = 5701;
		
		@XmlTransient
		public ClientConfig getClientConfig() {
			ClientConfig cc = new ClientConfig();
			for (String address:hostList)
				cc.addInetSocketAddress(new InetSocketAddress(address, port));
			cc.setGroupConfig(groupConfig);
			return cc;
		}
	}
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
	
	private int port = 5701; 

	private GroupConfig groupConfig = new GroupConfig();
	
	private String clusterName = "retry_client_cluster";

	
	public RemoteXmlConfig() {}
	
	public RemoteXmlConfig(List<String> addressList) {
		this.addressList = addressList;
	}
	public void add(String address) {
		this.addressList.add(address);
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
	
	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
	@XmlTransient
	public ClientConfig getHzClientConfig() {
		ClientConfig cc = new ClientConfig();
		
		cc.addAddress(addressList.toArray(new String[]{}));
		
		cc.setGroupConfig(groupConfig);
		
		return cc;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
}
