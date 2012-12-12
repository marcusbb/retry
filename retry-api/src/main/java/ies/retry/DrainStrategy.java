package ies.retry;

/**
 * The strategy for which the queued retries will be drained.
 * 
 * FIFO - this strategy will only allow the first queued to be drained before any other can be tried.
 * UNORDERED - similar to the previous PRV incarnation will attempt to callback according
 * to the {@link RetryConfiguration}
 * 
 * As un-ordered is the requirement at this time, this will be the only current
 * supported drain strategy.
 * 
 * @author msimonsen
 *
 */
public enum DrainStrategy {

	//NO FIFO IMPLEMENTATION EXISTS
	FIFO, 
	UNORDERED;
}
