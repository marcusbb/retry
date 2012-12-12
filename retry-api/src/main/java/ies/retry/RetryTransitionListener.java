package ies.retry;


/**
 * Clients may be interested global retry transitions.
 * 
 * 
 * 
 * This is particularly important when Retry transitions
 * from a state of retrying.
 * 
 * A particular use case:
 *  - retry is queued 
 *  -all clients are informed of a {@link RetryTransitionEvent.Type#QUEUED} event
 *  -clients may decide that due to ordering principles that all further notifications
 *  are queued through retries through {@link RetryManager#addRetry(Retry)}
 * - when the {@link RetryTransitionEvent.Type#DRAINED} event arrives the client
 * then will determine it can carry on with regular processing. 
 * 
 * @author msimonsen
 *
 */
public interface RetryTransitionListener {

	public void onEvent(RetryTransitionEvent event);
}
