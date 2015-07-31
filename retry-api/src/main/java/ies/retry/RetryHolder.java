package ies.retry;


import java.io.Serializable;
import java.util.Date;

/**
 * The holder and meta data wrapping the retryable object {@link #retryData}.
 * 
 * id and type IS required.
 * 
 * @author msimonsen
 *
 */
public class RetryHolder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1460592366899313710L;

	
	/**
	 * unique identifier for this retry
	 * this will be identified by the client
	 * adding to an already existing id will override the retry/add to the list of retry holders
	 */
	
	String id;
	
	/**
	 * This is the type or could be the endpoint of the failing connection
	 */
	String type;
	
	/**
	 * the timestamp for this retry
	 * it doesn't need to be set as it will be generated for the client
	 */
	long systemTs = System.currentTimeMillis();
	
	long nextAttempt = systemTs;
	
	
	/**
	 * The content of the data.
	 * Depending on persistence concerns this may be 
	 * encoded/compressed - and may also have an upper bound in terms
	 * of size.
	 * 
	 */
	transient Serializable retryData;
	
	private byte [] payload;
	/**
	 * the exception encountered
	 * The entire stack may not be persisted due to memory
	 * or persistence concerns
	 * 
	 * Due to serialization concerns, the client may not reliably
	 * call a {@link Exception#getStackTrace()} operation
	 * 
	 */
	transient Exception exception;
	
	private byte [] exPayload;
	
	/**
	 * Completely optional secondary index
	 */
	String secondaryIndex;
	
	/**
	 * the number of times that an attempt has been made.
	 * For a {@link DrainStrategy#FIFO} the count will always be
	 * zero for every retry except first queued.
	 * 
	 */
	int count = 0;

	public RetryHolder(String id,String type) {
		this(id,type,null,null);
	}
	
	public RetryHolder(String id,String type,Exception e) {
		this(id,type,e,null);
	}
	
	public RetryHolder(String id,String type,Exception e,Serializable retryData) {
		this.id = id;
		this.type = type;
		this.exception = e;
		this.retryData = retryData;
		
	}
	
	public RetryHolder(String id,String type, Serializable retryData) {
		this(id,type,null,retryData);
	}
	
	


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getSystemTs() {
		return systemTs;
	}

	public void setSystemTs(long systemTs) {
		this.systemTs = systemTs;
	}

	public Serializable getRetryData() {
		return retryData;
	}

	public void setRetryData(Serializable retryData) {
		this.retryData = retryData;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	public void incrementCount() {
		this.count++;
	}

	public long getNextAttempt() {
		return nextAttempt;
	}

	public void setNextAttempt(long nextAttempt) {
		this.nextAttempt = nextAttempt;
	}

	
	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public String getSecondaryIndex() {
		return secondaryIndex;
	}

	public void setSecondaryIndex(String secondaryIndex) {
		this.secondaryIndex = secondaryIndex;
	}

	public String toString() {
		return "RetryHolder [id=" + id + ", type=" + type + ", systemTs="
				+ new Date(systemTs) + ", nextAttempt=" + new Date(nextAttempt) + 
				", retryData=" + retryData + ", exception="
				+ exception + ", count=" + count + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		RetryHolder comp = (RetryHolder)obj;
		return (comp.getId().equals(id) && comp.getType().equals(type));
	}
  
}
