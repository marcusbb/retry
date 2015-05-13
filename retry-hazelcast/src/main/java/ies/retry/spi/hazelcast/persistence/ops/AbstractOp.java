package ies.retry.spi.hazelcast.persistence.ops;

import ies.retry.RetryHolder;

import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;

import provision.services.logging.Logger;

/**
 * A jpa transactable task.
 * 
 * @author msimonsen
 *
 * @param <T>
 */
public abstract class AbstractOp<T> implements Callable<OpResult<T>>,OpResult<T>{
	
	private String caller = this.getClass().getName();

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
	public OpResult<T> call() throws Exception {
		EntityManager em = emf.createEntityManager();
		
		long maxTimes = 5; // we try to re-run operation maxTimes if primary key of retries tables was violated
		
		for (int i = 0; i < maxTimes; i++) {
			try {
				em.getTransaction().begin();
				//T ret =
				exec(em);
				em.flush();
				em.getTransaction().commit();
				return this;  // success, going out of the loop
			}
			catch (ConstraintViolationException e) {
				if(i == maxTimes-1){ // log error if we failed to execute Op maxTimes
					Logger.error(caller, "Persistence_Op_PK_Exception", "DB operation failed because of PK violation: " + e.getMessage(), e); // log error message
				}
				else{
					Logger.warn(caller, "Persistence_Op_PK_Exception", "DB operation failed because of PK violation: " + e.getMessage(), e); // just log message, going to next iteration
				}
			}
			catch (PersistenceException e) {
				Logger.error(caller, "Persistence_Op_Exception", "Exception Message: " + e.getMessage(), e);
				throw e; // unknown persistence exception, leaving the loop
			} catch (Exception e) {
				Logger.error(caller, "Persistence_Op_Exception", "Exception Message: " + e.getMessage(), e);
				break; // unknown exception, leaving the loop
			} finally {
				em.getTransaction().rollback();
			}

		}
		
		return null;
	}
	
	public abstract T exec(EntityManager em) throws Exception;
	
	
	

}
