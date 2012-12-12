package ies.retry;


/**
 * When will the retry is triggered, this is the callback that
 * event that will be triggered.
 * 
 * @author msimonsen
 *
 */
public interface RetryCallback {

	/**
	 * Depending on a schedule - 
	 * Return true on a successful retry attempt
	 *  
	 * due to a potential configuration - {@link RetryConfiguration#isListBacked()}
	 * call back will be sequential for duplicated retry ids.
     * On exception synchronous callback for this id will be halted and called back
     * on the next pass.
	 * 
	 * @param retry
	 */
	public boolean onEvent(RetryHolder retry) throws Exception;
}
