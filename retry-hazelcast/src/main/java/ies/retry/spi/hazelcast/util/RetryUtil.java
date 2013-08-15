package ies.retry.spi.hazelcast.util;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HazelcastRetryImpl;
import ies.retry.spi.hazelcast.StateManager;
import ies.retry.spi.hazelcast.StateManager.LoadingState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.hazelcast.core.IMap;

import provision.services.logging.Logger;

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
				Long key = new Long(r.getSystemTs());
				if(!mergedMap.containsKey(key)) // objects from lists at the left side of argument list are supposed to have priority (because of retry count, etc)  
					mergedMap.put(key, r);
			}
		}
		
		List<RetryHolder> mergedList = new ArrayList<RetryHolder>(mergedMap.keySet().size());
		Iterator<Long> it = mergedMap.keySet().iterator();
		
		while(it.hasNext()){
			mergedList.add(mergedMap.get(it.next()));
		}
		
		return mergedList;

	}
	// retains data from failed and delta from latest lists. retry count for objects present in both list is supposed to be the same as in failed list
	public static List<RetryHolder> merge(String caller, List<RetryHolder> original, List<RetryHolder> failed, List<RetryHolder> latest) {
		
		
		if(latest==null || latest.size()==0 || latest.size()==original.size()){
			Logger.debug(caller, "Merging_retries: no new retries");
			return failed;
		}
		
		Set<Long> origTSs = new HashSet<Long>();
		for(int i=0; i<original.size(); i++){
			origTSs.add(original.get(i).getSystemTs());
		}
		
		//Make sure that retries from failed holder end up in merge result because counters were incremented
		Iterator<RetryHolder> it = latest.iterator();
		while(it.hasNext()){
			if(origTSs.contains(it.next().getSystemTs()))
				it.remove();
		}
		

		List<RetryHolder> result  = null;
		if(failed.size()!=0){ // new retries must be added to failed list
			result  = RetryUtil.merge(failed, latest); // merge orders items by timestamp
			Logger.debug(caller, "Merging_retries: failed size=" + failed.size() + ", latest size=" + latest.size() + ", result size=" + result.size());
		}
		else{ // only new retries should remain
			
			result = latest;
			Logger.debug(caller, "Merging_retries: original size=" + original.size() + ", latest size=" + latest.size() + ", result size=" + result.size());
		}
		
		return result;
	}
	public static boolean hasLoaded(String type){
			IMap<String, LoadingState> loadStateMap = ((HazelcastRetryImpl)Retry.getRetryManager()).getHzInst().getMap(StateManager.DB_LOADING_STATE);
			
			if( (loadStateMap == null) || (loadStateMap.get(type) == LoadingState.LOADING)){
				return false;
			}
			return true;
	}

	/*
	// removes entries present in the first list from second one. We have to do it manually because RetryHolder.equals() considers all items in the list being the same
	public static List<RetryHolder> removeExpired(String caller, List<RetryHolder> processed, List<RetryHolder> latest) {
		
		if(latest==null || latest.size()==0){
			Logger.debug(caller, "Merging_retries: no new retries");
			return Collections.EMPTY_LIST;
		}
		
		Set<Long> origTSs = new HashSet<Long>();
		for(int i=0; i<processed.size(); i++){
			origTSs.add(processed.get(i).getSystemTs());
		}
		
		//Make sure that retries from processed holder end up in merge result because counters were incremented
		Iterator<RetryHolder> it = latest.iterator();
		while(it.hasNext()){
			if(origTSs.contains(it.next().getSystemTs()))
				it.remove();
		}
		
		return latest;
	}
*/
}
