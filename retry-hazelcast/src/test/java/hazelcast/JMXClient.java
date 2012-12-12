package hazelcast;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXClient {

	public static void main(String []args) throws Exception{
		String host = args[0];
		JMXServiceURL u = new JMXServiceURL(
				  "service:jmx:rmi:///jndi/rmi://"+ host + "/jmxrmi");
		JMXConnector c = JMXConnectorFactory.connect(u);
		MBeanServerConnection mbs = c.getMBeanServerConnection();
		
		TestRetryAddMBean testMBean = JMX.newMBeanProxy(mbs, new ObjectName("retry:test=TestRetryAdd"), TestRetryAddMBean.class);
	
		Integer retryNum = Integer.parseInt(args[1]);
		
		testMBean.addRetry(retryNum);
		
		System.out.println("Added reties: " + retryNum);
		System.exit(0);
		
	}
}
