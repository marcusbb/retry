package ies.retry.spi.hazelcast.query;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;


/**
 * Takes a slice of each nodes retry.
 * 
 * @author msimonsen
 *
 */
public class SampleQuery implements BaseQuery {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7766002052617506174L;
	private int startIndex;
	private int maxSize;
	private String mapName;
	
	public SampleQuery(String mapName,int startindex,int maxSize) {
		this.startIndex = startindex;
		this.maxSize = maxSize;
		this.mapName = mapName;
	}
	@Override
	public QueryResults eval() {
		QueryResults results = new QueryResults();
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		HazelcastInstance h1 = ((HazelcastRetryImpl)Retry.getRetryManager()).getH1();
		results.setMember(h1.getCluster().getLocalMember());
		IMap<String,List<RetryHolder>> map = h1.getMap(mapName);
		Iterator<String> iter = map.localKeySet().iterator();
		int i = 0;
		while(iter.hasNext() && i<startIndex) {
			i++; iter.next();
		}
		i=0;
		while (iter.hasNext() && i<maxSize) {
			List<RetryHolder> rl = map.get(iter.next());
			list.addAll(rl); i++;
		}
		results.setResults(list);
		
		return results;
	}


	@Override
	public String getMapName() {
		return	mapName; 
	}
	@Override
	public Integer maxResultPerNode() {
		return maxSize;
	}
	
	
	
}
