package ies.retry.spi.hazelcast.util;

import ies.retry.xml.XMLRetryConfigMgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import provision.services.logging.Logger;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.impl.CMap;


public class HzUtil {

	public static String HZ_CONFIG_FILE = "hazelcast.xml";
	public static String HZ_PROP_FILE = "hz.properties";
	
	
	static String CALLER = HzUtil.class.getName();
	
	public static HazelcastInstance loadHzConfiguration() {
		//XMLRetryConfigMgr xmlconfigMgr = (XMLRetryConfigMgr)configMgr;
		HazelcastInstance h1 = null;
		try {
			
			Config config = loadHzConfig();
			
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Loaded Hazelcast: " + config.toString());
											
			h1 = Hazelcast.newHazelcastInstance(config);
		}catch (Exception e) {
			Logger.warn(CALLER, "Load_Hazelcast_Configuration", "NO HAZELCAST CONFIGURATION FOUND: " + e.getMessage(), e);
			h1 = Hazelcast.newHazelcastInstance();
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Using default config");
		}	
		return h1;
	}
	public static HazelcastInstance buildHzInstanceWith(String name) {
		
		try {
			Config config = loadHzConfig();
			config.setInstanceName(name);
			return Hazelcast.newHazelcastInstance(config);
		}catch (IOException e) {
			Logger.warn(CALLER, "Load_Hazelcast_Configuration", "NO HAZELCAST CONFIGURATION FOUND: " + e.getMessage(), e);
			Config c = new Config();
			c.setInstanceName(name);
			return Hazelcast.newHazelcastInstance(c);
		}
		
		
		
	}

	private static Config loadHzConfig() throws IOException {
		
			Config config = null;
			String dir = XMLRetryConfigMgr.getCONFIG_DIR();
			if (!"".equals(dir)) {
				String fileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_CONFIG_FILE;
				//config = new FileSystemXmlConfig(fileName);
				String xml = IOUtil.load(fileName);
				xml = StringUtil.replace(xml, System.getProperties());
				config = new InMemoryXmlConfig(xml); 
				//load in the system parameters:
				String propFileName = XMLRetryConfigMgr.getCONFIG_DIR() + System.getProperty("file.separator") + HZ_PROP_FILE;
				File propFile = new File(propFileName);
				if (propFile.exists()) {
					FileInputStream propFin = new FileInputStream(propFile);
					Properties properties = new Properties();
					properties.load(propFin);
					for (Object key:properties.keySet()) {
						Logger.info(CALLER, "Load_Hz_Prop","load","key",key,"value",properties.get(key));
						System.setProperty((String)key, (String)properties.get(key) );
					}
				}else {
					Logger.info(CALLER, "Load_Hz_Prop","No Properties to set");
				}
				
			}else {
				config = new ClasspathXmlConfig(HZ_CONFIG_FILE);
			}
			
			Logger.info(CALLER, "Load_Hazelcast_Configuration", "Loaded Hazelcast: " + config.toString());
			
			return config;
		
	}
	public int maxMapSize(MapConfig mapConfig,Cluster cluster) {
		MaxSizeConfig maxSizeConfig = mapConfig.getMaxSizeConfig();
		MapMaxSizePolicy maxSizePolicy = null;
		
		if (MaxSizeConfig.POLICY_MAP_SIZE_PER_JVM.equals(maxSizeConfig.getMaxSizePolicy())) {
            maxSizePolicy = new MaxSizePerJVMPolicy(maxSizeConfig);
        } else if (MaxSizeConfig.POLICY_CLUSTER_WIDE_MAP_SIZE.equals(maxSizeConfig.getMaxSizePolicy())) {
            maxSizePolicy = new MaxSizeClusterWidePolicy(maxSizeConfig,cluster);
        } else if (MaxSizeConfig.POLICY_PARTITIONS_WIDE_MAP_SIZE.equals(maxSizeConfig.getMaxSizePolicy())) {
            maxSizePolicy = new MaxSizePartitionsWidePolicy(maxSizeConfig);
        } else if (MaxSizeConfig.POLICY_USED_HEAP_SIZE.equals(maxSizeConfig.getMaxSizePolicy())) {
            maxSizePolicy = new MaxSizeHeapPolicy(maxSizeConfig);
        } else if (MaxSizeConfig.POLICY_USED_HEAP_PERCENTAGE.equals(maxSizeConfig.getMaxSizePolicy())) {
            maxSizePolicy = new MaxSizeHeapPercentagePolicy(maxSizeConfig);
        } else {
        	// protected against this?
            maxSizePolicy = null;
        }
		return maxSizePolicy.getMaxSize();
		
	}
	
	
	/**
	 * Following class definitions ripped from {@link CMap}
	 * 
	 *
	 */
	public interface MapMaxSizePolicy {

	    //boolean overCapacity();

	    //MaxSizeConfig getMaxSizeConfig();
	    
	    int getMaxSize();

	}
	 class MaxSizePerJVMPolicy implements MapMaxSizePolicy {
	        protected final MaxSizeConfig maxSizeConfig;

	        MaxSizePerJVMPolicy(MaxSizeConfig maxSizeConfig) {
	            this.maxSizeConfig = maxSizeConfig;
	        }

	        public int getMaxSize() {
	            return maxSizeConfig.getSize();
	        }

	        public boolean overCapacity() {
	           throw new UnsupportedOperationException("");
	        }

	        public MaxSizeConfig getMaxSizeConfig() {
	            return maxSizeConfig;
	        }
	    }

	    class MaxSizeHeapPolicy extends MaxSizePerJVMPolicy {
	        final long memoryLimit;

	        MaxSizeHeapPolicy(MaxSizeConfig maxSizeConfig) {
	            super(maxSizeConfig);
	            memoryLimit = maxSizeConfig.getSize() * 1024L * 1024L; // MB to byte
	        }

	        public boolean overCapacity() {
	        	throw new UnsupportedOperationException("");
	        }
	    }

	    class MaxSizeHeapPercentagePolicy extends MaxSizePerJVMPolicy {
	        final int maxPercentage;

	        MaxSizeHeapPercentagePolicy(MaxSizeConfig maxSizeConfig) {
	            super(maxSizeConfig);
	            maxPercentage = maxSizeConfig.getSize();
	        }

	        public boolean overCapacity() {
	        	throw new UnsupportedOperationException("");
	        }
	    }

	    class MaxSizeClusterWidePolicy extends MaxSizePerJVMPolicy {
	    	Cluster cluster;
	    	
	        MaxSizeClusterWidePolicy(MaxSizeConfig maxSizeConfig,Cluster cluster) {
	            super(maxSizeConfig);
	        }

	        @Override
	        public int getMaxSize() {
	            final int maxSize = maxSizeConfig.getSize();
	            final int clusterMemberSize = cluster.getMembers().size();
	            final int memberCount = (clusterMemberSize == 0) ? 1 : clusterMemberSize;
	            return maxSize / memberCount;
	        }
	    }

	    class MaxSizePartitionsWidePolicy extends MaxSizePerJVMPolicy {

	        MaxSizePartitionsWidePolicy(MaxSizeConfig maxSizeConfig) {
	            super(maxSizeConfig);
	        }

	        @Override
	        public int getMaxSize() {
	           throw new UnsupportedOperationException("");
	        }
	    }

}
