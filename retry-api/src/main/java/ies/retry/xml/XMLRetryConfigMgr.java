package ies.retry.xml;

import ies.retry.ConfigException;
import ies.retry.RetryConfigManager;
import ies.retry.RetryConfiguration;
import ies.retry.spi.stub.StubbedRetryManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class XMLRetryConfigMgr implements RetryConfigManager{

	public static String OBJ_FACTORY_PROP = "com.sun.xml.bind.ObjectFactory";
	public static String XML_FILE = "retry_config.xml";
	private Map<String,RetryConfiguration> configMap;
	XmlRetryConfig config = null;
	private Class<? extends XmlRetryConfig> jaxbConfigClass = XmlRetryConfig.class; 
	private ConfigObjectFactory factory = new ConfigObjectFactory();
	
	private static String CONFIG_DIR = "";
	
	JAXBContext jc = null;
			
	public XMLRetryConfigMgr() {
				
		this.configMap = new HashMap<String, RetryConfiguration>();
	}
	/**
	 * Loads the configuration from ./ relative directory
	 * if not found then loads from classpath
	 * 
	 * Expensive - should be a one time operation.
	 * 
	 * @throws JAXBException 
	 * 
	 */
	public  XmlRetryConfig load() throws IOException, JAXBException {
		File file = new File(CONFIG_DIR + System.getProperty("file.separator") + XML_FILE);
		InputStream ins = null;
		if (file.exists()) {
			ins = new FileInputStream(file);
		}
		else {
			ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(XML_FILE);
		}
		if (ins == null) {
			System.out.println("WARNING: ---------------------------------------------");
			System.out.println("WARNING: A retry configuration file has not been found");
			System.out.println("WARNING: Reverting to Stubbed implementation");
			System.out.println("WARNING: ---------------------------------------------");
			XmlRetryConfig defConfig = new XmlRetryConfig();
			defConfig.setProvider(StubbedRetryManager.class.getName());
			
		}
		//jc = JAXBContext.newInstance(jaxbConfigClass);
		jc = JAXBContext.newInstance(jaxbConfigClass);
		
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		
		
		JAXBElement<? extends XmlRetryConfig> jaxbElement = (JAXBElement<? extends XmlRetryConfig>)unmarshaller.unmarshal(new StreamSource(ins),jaxbConfigClass);
		config = jaxbElement.getValue();		
		//System.out.println("Configuration class: " + jaxbElement.getValue().getClass());
		for (RetryConfiguration retryConfig: config.getTypeConfig()) {
			configMap.put(retryConfig.getType(), retryConfig);
		}
		return config;
	}
	public synchronized void load(String xml) throws JAXBException{
		jc = JAXBContext.newInstance(jaxbConfigClass);
		StringReader reader = new StringReader(xml);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		JAXBElement<? extends XmlRetryConfig> jaxbElement = (JAXBElement<? extends XmlRetryConfig>)unmarshaller.unmarshal(new StreamSource(reader),jaxbConfigClass);
		config = jaxbElement.getValue();
		
		this.configMap.clear();
		for (RetryConfiguration retryConfig: config.getTypeConfig()) {
			configMap.put(retryConfig.getType(), retryConfig);
		}
	}
	public String marshallXML() throws JAXBException {
		if (jc == null)
			throw new RuntimeException("You haven't called load yet");
		Marshaller marshaller = jc.createMarshaller();
		//marshaller.setProperty(OBJ_FACTORY_PROP,factory);
		StringWriter sw = new StringWriter();
		marshaller.marshal(config, sw);
		//System.out.println("XML: " + sw.toString());
		return sw.toString();
	}
	/**
	 * Clone by java serialization
	 */
	public RetryConfiguration cloneConfiguration(String type) {
		RetryConfiguration toClone = configMap.get(type);
		RetryConfiguration cloned = null;
		if (toClone != null) {
			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bout);
				oos.writeObject(toClone);
				ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
				
				cloned = (RetryConfiguration)oin.readObject();
			}catch (Exception e) {
				throw new ConfigException(e.getMessage(),e);
			}finally {
				//TODO: IO cleanup
			}
			
		}
		
		return cloned;
		
	}
	/**
	 * This may be a distributed operation
	 */
	public void addConfiguration(RetryConfiguration config) {
		configMap.put(config.getType(), config);
	}

	public Collection<RetryConfiguration> getAll() {
		return configMap.values();
	}
	
	public RetryConfiguration getConfiguration(String type) {
		return configMap.get(type);
	}
	public XmlRetryConfig getConfig() {
		return config;
	}
	public void setConfig(XmlRetryConfig config) {
		this.config = config;
	}
	public Map<String, RetryConfiguration> getConfigMap() {
		return configMap;
	}
	public void setConfigMap(Map<String, RetryConfiguration> configMap) {
		this.configMap = configMap;
	}
	
	public ConfigObjectFactory getFactory() {
		return factory;
	}
	public void setFactory(ConfigObjectFactory factory) {
		this.factory = factory;
	}
	public Class<? extends XmlRetryConfig> getJaxbConfigClass() {
		return jaxbConfigClass;
	}
	public void setJaxbConfigClass(Class<? extends XmlRetryConfig> jaxbConfigClass) {
		this.jaxbConfigClass = jaxbConfigClass;
	}
	public static String getXML_FILE() {
		return XML_FILE;
	}
	public static void setXML_FILE(String xML_FILE) {
		XML_FILE = xML_FILE;
	}
	public static String getCONFIG_DIR() {
		return CONFIG_DIR;
	}
	public static void setCONFIG_DIR(String cONFIG_DIR) {
		CONFIG_DIR = cONFIG_DIR;
	}
	
	
	
}
