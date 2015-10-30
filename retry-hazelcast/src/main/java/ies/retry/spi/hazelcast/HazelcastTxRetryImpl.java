package ies.retry.spi.hazelcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import ies.retry.ConfigException;
import ies.retry.NoCallbackException;
import ies.retry.RetryHolder;
import ies.retry.TxRetryManager;

/**
 * Retry with transaction capabilities
 * 
 * @author marcus
 *
 */
public class HazelcastTxRetryImpl extends HazelcastRetryImpl implements Synchronization,TxRetryManager {

	//done with a thread local, which is most performant and cheapest but not durable
	//add doesn't support transactions that spans multiple threads 
	private static final ThreadLocal<Map<String,List<RetryHolder>>> txMap = new ThreadLocal<>();
	
	public HazelcastTxRetryImpl() {
		super();
		
	}

	@Override
	public void addRetry(RetryHolder holder) {
		
		
		if (txMap.get() == null) {
			//throw new IllegalStateException("Transaction hasn't been registerd");
			//warn of non-pending transaction status
			super.addRetry(holder);
			return;
			
		}
		
		if (txMap.get().get(holder.getId()) == null) {
			List<RetryHolder> l = new ArrayList<>();
			l.add(holder);
			txMap.get().put(holder.getId(), l);
		}else {
			txMap.get().get(holder.getId()).add(holder);
		}
	}
	
	
	@Override
	public void putRetry(List<RetryHolder> retryList) throws NoCallbackException, ConfigException {
		
		if (txMap.get() == null) {
			//throw new IllegalStateException("Transaction hasn't been registerd");
			//warn of non-pending transaction status
			super.putRetry(retryList);
			return;
		}
		if (retryList == null || retryList.get(0) == null) {
			throw new IllegalArgumentException("Empty retry list");
		}
		//non-additive, just overrides
		txMap.get().put(retryList.get(0).getId(), retryList);
		 
		
	}
	

	@Override
	public void afterCompletion(int status) {
		
		if (status == Status.STATUS_COMMITTED) {
			//This is where we don't guarantee 
			//things will be added with absolute certainty
			for (List<RetryHolder> retryList: txMap.get().values()) {
				for (RetryHolder holder:retryList) {
					super.addRetry(holder);
				}
			}
		}
		txMap.set(null);
	}

	@Override
	public void beforeCompletion() {
		
		
	}

	@Override
	public void syncWith(Transaction tx) throws SystemException, RollbackException {
		tx.registerSynchronization(this);
		txMap.set(new HashMap<String,List<RetryHolder>>());
	}

	
	
}
