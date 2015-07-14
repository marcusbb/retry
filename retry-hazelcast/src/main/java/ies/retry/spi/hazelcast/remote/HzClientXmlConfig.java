package ies.retry.spi.hazelcast.remote;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.config.GroupConfig;

import ies.retry.xml.XmlRetryConfig;

@XmlRootElement(name="retry")
public class HzClientXmlConfig extends XmlRetryConfig {

	
	private static final long serialVersionUID = -7332822405707330770L;
	
	private String clusterName = "retry_client_cluster";

	
	private HzClientXmlConfig.SeralizableClientConfig remoteClusterConfig;
	
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
	
	public HzClientXmlConfig() { }
	
	
	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}


	public HzClientXmlConfig.SeralizableClientConfig getRemoteClusterConfig() {
		return remoteClusterConfig;
	}


	public void setRemoteClusterConfig(
			HzClientXmlConfig.SeralizableClientConfig remoteClusterConfig) {
		this.remoteClusterConfig = remoteClusterConfig;
	}


	
	
	
}
