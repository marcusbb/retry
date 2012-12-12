package ies.retry;

import java.io.Serializable;

/**
 * Extra batch type configuration for
 * concurrent retry callback. 
 * 
 * Can only exist inside {@link RetryConfiguration}.
 * 
 * It also controls the independent timers 
 * 
 * @author msimonsen
 *
 */
public class BatchConfig implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7282974832277688977L;

	/**
	 * The number of concurrent retries executed in a batch
	 * This will be now considered the "max" size
	 *  
	 */
	private int batchSize = 10;
	
	/**
	 * This is the minimum concurrent batch size.
	 * For fixed size set it to {@link #batchSize}
	 */
	private int minBatchSize = 1;
	
	
	
	
	
	/**
	 * The subsequent interval at which batch will be processed.
	 * Each scheduler node will have an independent 
	 * timer, and it's allocated retries at this rate.
	 * 
	 * 
	 */
	private long batchHeartBeat = 2000;
	
	
	
	
	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public long getBatchHeartBeat() {
		return batchHeartBeat;
	}

	public void setBatchHeartBeat(long batchHeartBeat) {
		this.batchHeartBeat = batchHeartBeat;
	}

	public int getMinBatchSize() {
		return minBatchSize;
	}

	public void setMinBatchSize(int minBatchSize) {
		this.minBatchSize = minBatchSize;
	}

	

	

	
	
	
	
	
	
}

