package ies.retry.spi.hazelcast;

import java.io.Serializable;

import ies.retry.spi.hazelcast.config.PersistenceConfig;

/**
 * An exception when storage fails to timeout in the allocated timeout.
 * See {@link PersistenceConfig#getTimeoutInms()}
 * 
 * @author msimonsen
 *
 */
public class StoreTimeoutException extends RuntimeException implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -277955486497422022L;

	public StoreTimeoutException() {
		super();
		
	}

	public StoreTimeoutException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}

	public StoreTimeoutException(String arg0) {
		super(arg0);
		
	}

	public StoreTimeoutException(Throwable arg0) {
		super(arg0);
		
	}

	
	

}
