package ies.retry;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Can only exist for any given {@link RetryConfiguration} 
 * 
 * the properties for determining the algorithm for Backoff.
 * 
 * 
 * the retry attempts will be trying according to:
 *  {@link #maxAttempts} >= {@link RetryHolder#count}
 *  
 *  With each attempt calculated at:
 *  {@link RetryHolder#getNextAttempt()} = {@link #interval} 
 *  
 *  OR
 *  
 *  A more static configuration can be supplied, similar to current PRV application,
 *  where the next attempt will be calculated:
 *  attempt_ts = staticIntervals[ {@link BackOff#count} ]
 *  
 *   This can turn into selection policy.
 * One based on next time to retry each {@link RetryHolder}
 * 
 * For suspensions - for each retry that has a "past" date
 * will be retried immediately on resume.
 * Including scheduler shutdown.
 *  
 *  
 * @author msimonsen
 *
 *
 * 
 * 
 */
public class BackOff implements Serializable {

	public enum BackoffMode {
		StaticIntervals,
		Periodic,
		Geometric
	};	
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -1451268305484545998L;

	/**
	 * The absolute number of attempts for any type.
	 */
	private int maxAttempts = 10; //

	/**
	 * The millis between successive callback attempts {@link RetryCallback}
	 * 
	 */
	private long interval = 60 * 1000; //min interval

	/**
	 * combined with the {@link #interval} will determine the actual backoff.
	 * If < 1 then it will be shorter period - hence not a back off.
	 */
//	@Deprecated
	private float intervalMultiplier = 1;
	
	private List<Long> staticIntervals;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		
	private BackoffMode backoffMode = BackoffMode.Periodic;
	

	public void setBackoffMode(BackoffMode backoffMode) {
		this.backoffMode = backoffMode;
	}
	
	public BackoffMode getBackoffMode()	{	
		return backoffMode;
	}

	public int getMaxAttempts() {
		
		return maxAttempts;
	}


	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}


	public long getInterval() {
		return interval;
	}


	public void setInterval(long interval) {
		this.interval = interval;
	}

//	@Deprecated
	public float getIntervalMultiplier() {
		return intervalMultiplier;
	}


	public void setIntervalMultiplier(float intervalMultiplier) {
		this.intervalMultiplier = intervalMultiplier;
	}

	public List<Long> getStaticIntervals() {
		return staticIntervals;
	}


	public void setStaticIntervals(List<Long> staticIntervals) {
		this.staticIntervals = staticIntervals;
	}


	public TimeUnit getTimeUnit() {
		return timeUnit;
	}


	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public long getMilliInterval() {
		long milliInterval = TimeUnit.MILLISECONDS.convert(interval, this.timeUnit);

		return milliInterval;
	}

	public long []staticMillis() {
		long []milliIntervals = null;
		if (staticIntervals != null) {
			milliIntervals = new long[staticIntervals.size()];
			for (int i=0;i<staticIntervals.size();i++)
				milliIntervals[i] = timeUnit.toMillis(staticIntervals.get(i));
		}
		return milliIntervals;
	}
}
