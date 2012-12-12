package jaxb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import junit.framework.Assert;

import ies.retry.BackOff;
import ies.retry.BatchConfig;
import ies.retry.Retry;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.xml.ConfigObjectFactory;
import ies.retry.xml.XMLRetryConfigMgr;
import ies.retry.xml.XmlRetryConfig;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class MiscTest {

	protected static JAXBContext jc ;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		jc = JAXBContext.newInstance(XmlRetryConfig.class);
	}
	@Test
	public void testCreateXmlDoc() throws Exception {
		
		ConfigObjectFactory factory = new ConfigObjectFactory();
		XmlRetryConfig xmlconfig = factory.createRetryConfiguration();
		ArrayList<RetryConfiguration> configList = new ArrayList<RetryConfiguration>();
		xmlconfig.setTypeConfig(configList);
		/*Properties properties = new Properties();
		properties.setProperty("test","test");
		xmlconfig.setVendorConfig(properties);*/
		RetryConfiguration config = new RetryConfiguration();
		configList.add(config);
		BackOff backOff = new BackOff();
		backOff.setInterval(5000);
		config.setBackOff(backOff);
		
		BatchConfig batchConfig = new BatchConfig();
		batchConfig.setBatchHeartBeat(1000);
		batchConfig.setBatchSize(1000);
		config.setBatchConfig(batchConfig);
		
		config.setBackOff(backOff);
		config.setArchiveExpired(true);
		
		FileWriter fw = new FileWriter(new File("config.xml"));
		
		Marshaller marshaller = jc.createMarshaller();
		
		
		marshaller.marshal(xmlconfig, fw);
		
		fw.close();
	}
	
	
	// TODO ak: We can implement equals in RetryConfiguration and compare objects saved to XML to one loaded from it 
	@Test
	public void testCreateAndReadXmlDoc() throws Exception {
		
		// create file
		ConfigObjectFactory factory = new ConfigObjectFactory();
		XmlRetryConfig xmlconfig = factory.createRetryConfiguration();
		ArrayList<RetryConfiguration> configList = new ArrayList<RetryConfiguration>();
		xmlconfig.setTypeConfig(configList);
		RetryConfiguration config = new RetryConfiguration();
		configList.add(config);
		BackOff backOff = new BackOff();
		backOff.setInterval(5000);
		config.setBackOff(backOff);
		
		BatchConfig batchConfig = new BatchConfig();
		batchConfig.setBatchHeartBeat(1000);
		batchConfig.setBatchSize(1000);
		config.setBatchConfig(batchConfig);
		
		config.setBackOff(backOff);
		config.setArchiveExpired(true);
		
		File file = new File("config_readwrite.xml");
		FileWriter fw = new FileWriter(file);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.marshal(xmlconfig, fw);
		fw.close();
		
		Assert.assertTrue("Output XML was not created", file.exists());
		
		// read file
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		InputStream ins = new FileInputStream(file);
		
		xmlconfig = (XmlRetryConfig)unmarshaller.unmarshal(ins);
		config = xmlconfig.getTypeConfig().get(0);
		Assert.assertTrue("Archive expired is supposed to be true", config.isArchiveExpired());
	}
	
	
	@Test
	public void readConfig() throws Exception {
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream("config_1.xml");
		
		XmlRetryConfig config = (XmlRetryConfig)unmarshaller.unmarshal(ins);
		
		//String value = (String)config.getVendorProperties().get("test");
	}
	
	@Test
	public void readExtConfig() throws Exception {
		XMLRetryConfigMgr mgr = new XMLRetryConfigMgr();
		//XMLRetryConfigMgr.XML_FILE = "config_ext.xml";
		mgr.setJaxbConfigClass(ConfigExt.class);
		//XmlRetryConfig xmlconfig = mgr.load();
		
		mgr.setFactory(new ConfigExtObjFactory());
		mgr.load();
		ConfigExt config = (ConfigExt)mgr.getConfig();
		
		Assert.assertEquals("vendor_specific", config.getStrProperty());
		
		System.out.println(mgr.marshallXML() );
	}
	
	@Test
	@Ignore
	public void retryEntry() throws Exception {
		RetryConfigManager configMgr = Retry.getRetryManager().getConfigManager();
		Assert.assertNotNull("configMgr is null", configMgr);
		Assert.assertTrue("configMgr is instance of " + configMgr.getClass() + " while expected to be instance of XMLRetryConfigMgr", configMgr instanceof XMLRetryConfigMgr);
		Assert.assertTrue(
				((XMLRetryConfigMgr)configMgr).getConfig() instanceof XmlRetryConfig
				);
		
		XMLRetryConfigMgr mgr = new XMLRetryConfigMgr();
		mgr.setFactory(new ConfigExtObjFactory());
		XmlRetryConfig config = mgr.load();
		
	}

}
