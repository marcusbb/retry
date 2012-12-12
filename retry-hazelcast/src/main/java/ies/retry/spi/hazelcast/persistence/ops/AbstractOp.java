package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;

import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import provision.services.logging.Logger;

/**
 * A jpa transactable task.
 * 
 * @author msimonsen
 *
 * @param <T>
 */
public abstract class AbstractOp<T> implements Callable<T>{

	protected EntityManagerFactory emf;
	
	protected String retryType;
	
	protected List<RetryHolder> listHolder;
	
	protected String storeId;
	
	public AbstractOp() {
		
	}
	public AbstractOp(EntityManagerFactory emf){
		this.emf = emf;
	}
	public AbstractOp(EntityManagerFactory emf,List<RetryHolder>listHolder,String retryType,String storeId) {
		
		this.emf = emf;
		this.listHolder = listHolder;
		this.retryType = retryType;
		this.storeId = storeId;
	}
	
	public EntityManagerFactory getEmf() {
		return emf;
	}
	public void setEmf(EntityManagerFactory emf) {
		this.emf = emf;
	}
	public String getStoreId() {
		return storeId;
	}
	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}
	public List<RetryHolder> getListHolder() {
		return listHolder;
	}
	public void setListHolder(List<RetryHolder> listHolder) {
		this.listHolder = listHolder;
	}
	public String getRetryType() {
		return retryType;
	}
	public void setRetryType(String retryType) {
		this.retryType = retryType;
	}
	
	@Override
	public T call() throws Exception {
		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			
			T ret = exec(em);
			
			if (allowExceptionHandling()) {
				try {
					em.flush();
				}catch (PersistenceException e) {
					handleException(e);
				}
			}
			em.getTransaction().commit();
			return ret;
		} catch (PersistenceException e) {
			throw e;
		}catch (Exception e) {
			Logger.error(getClass().getName(), "Persistence_Op_Exception", "Exception Message: " + e.getMessage(), e);
		}
		return null;
	}
	
	public abstract T exec(EntityManager em) throws Exception;
	
	/**
	 * Gives chance for exception handling prior to commit,
	 * flushing resources before the commit is called.
	 * 
	 * @return
	 */
	public  boolean allowExceptionHandling() {
		return false;
	}
	/**
	 * handle the commit exception (flush)
	 * 
	 * By default just logs
	 * @param e
	 */
	public void handleException(PersistenceException e) throws PersistenceException {
		Logger.warn(getClass().getName(), "Persistence_Op_Handle_Exception", "Exception Message: " + e.getMessage(),e);
		throw e;
	}
}
