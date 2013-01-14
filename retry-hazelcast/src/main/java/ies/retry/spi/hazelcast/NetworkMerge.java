package ies.retry.spi.hazelcast;

import java.util.ArrayList;

import java.util.List;
import java.util.TreeSet;

import provision.services.logging.Logger;

import ies.retry.Retry;
import ies.retry.RetryConfiguration;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.config.HazelcastXmlConfig;
import ies.retry.spi.hazelcast.config.HazelcastConfigManager;
import ies.retry.spi.hazelcast.config.NetworkMergePolicy;
import ies.retry.spi.hazelcast.persistence.RetryMapStoreFactory;

import ies.retry.spi.hazelcast.util.TSHolderComparator;

import com.hazelcast.core.MapEntry;
import com.hazelcast.impl.base.DataRecordEntry;
import com.hazelcast.merge.MergePolicy;
import com.hazelcast.nio.Data;
import com.hazelcast.nio.IOUtil;

/**
 * Not used at all.
 * 
 * Should be removed.
 *
 */
public class NetworkMerge implements MergePolicy {

	private static final String CALLER = NetworkMerge.class.getName();
	static HazelcastRetryImpl retryInst = (HazelcastRetryImpl)Retry.getRetryManager();
	
	@Override
	public Object merge(String mapName, MapEntry merging, MapEntry existing) {
		
		Data retData = ((DataRecordEntry)existing).getValueData();
		
		try {
			HazelcastConfigManager configMgr = (HazelcastConfigManager)retryInst.getConfigManager();
			HazelcastXmlConfig config = (HazelcastXmlConfig)configMgr.getConfig();
			
			RetryConfiguration retryConfig = configMgr.getConfiguration(mapName);
			
			if (retryConfig == null)
				return ((DataRecordEntry)existing).getValueData();
			
			List<RetryHolder> dataMerging = (List<RetryHolder>)((DataRecordEntry)merging).getValue();
			List<RetryHolder>  dataExisting = (List<RetryHolder>)((DataRecordEntry)existing).getValue();
			
			//no other policies supported currently
			if (config.getPersistenceConfig().isON() && config.getMergePolicy() == NetworkMergePolicy.DB_OVERWRITE) {
				
				List<RetryHolder> dblist = RetryMapStoreFactory.getInstance().newMapStore(mapName).load((String)((DataRecordEntry)existing).getKey());
				retData =  IOUtil.toData(dblist);
						
			}else  {
				List<RetryHolder> mergedList = merge(dataMerging,dataExisting);
				
				retData = IOUtil.toData(mergedList);
			}
		}catch (Exception e) {
			Logger.error(CALLER, "Merge_Exception", "Exception Message: " + e.getMessage(), "Type", mapName, e);
		}
		return retData;
	}

	public static HazelcastRetryImpl getRetryInst() {
		return retryInst;
	}

	public static void setRetryInst(HazelcastRetryImpl retryInst) {
		NetworkMerge.retryInst = retryInst;
	}

	
	private List<RetryHolder> merge(List<RetryHolder> merging,List<RetryHolder> existing) {
		TreeSet<RetryHolder> mergeSet = new TreeSet<RetryHolder>(new TSHolderComparator());
		if (merging != null)
			mergeSet.addAll(merging);
		if (existing != null)
			mergeSet.addAll(existing);
		return new ArrayList<RetryHolder>(mergeSet);
	}
	
}
