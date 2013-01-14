package ies.retry.spi.hazelcast.util;

import ies.retry.RetryHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class RetryUtil {
	private RetryUtil() {
	}

	/*
	 * List must contain retries for the same id and type. Returned list will be
	 * ordered systemTS There is not way of telling whether retries with the
	 * same timestamp in memory and DB are the same. There is very small risk of
	 * collision.
	 */
	public static List<RetryHolder> merge(List<RetryHolder>... lists) {
		TreeMap<Long, RetryHolder> mergedMap = new TreeMap<Long, RetryHolder>();

		for (List<RetryHolder> list : lists) {
			if (list == null || list.size() == 0)
				continue;
			
			for (RetryHolder r : list) {
				mergedMap.put(new Long(r.getSystemTs()), r);
			}
		}
		
		List<RetryHolder> mergedList = new ArrayList<RetryHolder>(mergedMap.keySet().size());
		Iterator<Long> it = mergedMap.keySet().iterator();
		
		while(it.hasNext()){
			mergedList.add(mergedMap.get(it.next()));
		}
		
		return mergedList;

	}

}
