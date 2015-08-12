package ies.retry;

import java.io.Serializable;


/**
 * Retry configuration is broken down into type or category.
 * 
 * Each type will have it's own configuration, and can operate completely
 * independently of another types configuration.
 * 
 * May add other optimization configuration such as the amount of 
 * retries to hold in memory before storing.
 * 
 * All intervals in milliseconds unless specified.
 * 
 * @author msimonsen
 *
 */

public class RetryConfiguration implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1091841947535905344L;

	/**
	 * The category or type of message
	 */
	private String type;
	
	/**
	 * The strategy to back off.
	 * 
	 * For a FIFO implementation this only applies to the first element,
	 * 
	 * but for unordered it applies to all - where all
	 * elements will be retried with this back off algorithm.
	 * In this case the 
	 */
	private BackOff backOff;
	
	
	/**
	 * If listBacked - then duplicate retry with duplicate id's
	 * will stored according to the order of adds.
	 * 
	 */
	private boolean listBacked = true;
	
	/**
	 * Blocking retry add.  It will cost a network round trip to the 
	 * destination of the store
	 */
	private boolean syncRetryAdd = true;
	
	/**
	 * Is only timeout for add operation currently.
	 */
	private long syncTimeoutInms = 20 * 1000;
	
	/**
	 * Indicates the timeout period that retry will wait
	 * for execution of a callback
	 */
	private long callbackTimeoutMs = 5 * 60 * 1000;
	/**
	 *  
	 * Based on input on the review there is no need to drain FIFO.
	 * TODO: Remove {@link DrainStrategy#FIFO}
	 *  
	 * Infinispan implementation may not support a FIFO implementation.
	 * 
	 */
	private DrainStrategy queueStrategy = DrainStrategy.UNORDERED;
	
	/**
	 * Is the retry queue batch drained.
	 * 
	 * If set to null, retries will be drained as quickly as possible.
	 * 
	 * 
	 */
	private BatchConfig batchConfig = new BatchConfig();
	
	/**
	 * Indicates that this retry type is permanently stored
	 */
	private boolean persistenceOn = true;
	
	
	/**
	 *  Indicates whether archiving of expired retries is enabled
	 */
	private boolean archiveExpired;
	
	/**
	 *  Indicates which exception or cause exception in stack will be kept in RetryHolder
	 */	
	private int exceptionLevel;

	public int getExceptionLevel() {
		return exceptionLevel;
	}

	public void setExceptionLevel(int exceptionLevel) {
		this.exceptionLevel = exceptionLevel;
	}

	/**
	 *  Indicates how many stack trace line of Retry exception will be serialized
	 */
	private int stackTraceLinesCount;
	
	public int getStackTraceLinesCount() {
		return stackTraceLinesCount;
	}
	public void setStackTraceLinesCount(int stackTraceLinesCount) {
		this.stackTraceLinesCount = stackTraceLinesCount;
	}
		
	public boolean isArchiveExpired() {
		return archiveExpired;
	}

	public void setArchiveExpired(boolean archiveExpired) {
		this.archiveExpired = archiveExpired;
	}

	/**
	 * Max list size
	 */
	private int maxListSize = 12000; // Some large number
	
	public int getMaxListSize() {
		return maxListSize;
	}

	public void setMaxListSize(int maxListSize) {
		this.maxListSize = maxListSize;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BackOff getBackOff() {
		return backOff;
	}

	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}

	public DrainStrategy getQueueStrategy() {
		return queueStrategy;
	}

	public void setQueueStrategy(DrainStrategy queueStrategy) {
		this.queueStrategy = queueStrategy;
	}

	public BatchConfig getBatchConfig() {
		return batchConfig;
	}

	public void setBatchConfig(BatchConfig batchConfig) {
		this.batchConfig = batchConfig;
	}

	public boolean isListBacked() {
		return listBacked;
	}

	public void setListBacked(boolean listBacked) {
		this.listBacked = listBacked;
	}

	public boolean isSyncRetryAdd() {
		return syncRetryAdd;
	}

	public void setSyncRetryAdd(boolean syncRetryAdd) {
		this.syncRetryAdd = syncRetryAdd;
	}

	public boolean isPersistenceOn() {
		return persistenceOn;
	}

	public void setPersistenceOn(boolean persistenceOn) {
		this.persistenceOn = persistenceOn;
	}

	public long getSyncTimeoutInms() {
		return syncTimeoutInms;
	}

	public void setSyncTimeoutInms(long syncTimeoutInms) {
		this.syncTimeoutInms = syncTimeoutInms;
	}

	public long getCallbackTimeoutMs() {
		return callbackTimeoutMs;
	}

	public void setCallbackTimeoutMs(long callbackTimeoutMs) {
		this.callbackTimeoutMs = callbackTimeoutMs;
	}
	
	
	
}
