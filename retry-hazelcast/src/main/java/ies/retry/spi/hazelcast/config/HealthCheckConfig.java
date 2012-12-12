package ies.retry.spi.hazelcast.config;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class HealthCheckConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7790933619250671124L;

	private boolean enabled = false;
	
	//in millis
	//this is the interval at which we 
	private Integer checkInterval = 5000;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	
	private Integer checkMemeberInterval = 7000;
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(Integer checkInterval) {
		this.checkInterval = checkInterval;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public Integer getCheckMemeberInterval() {
		return checkMemeberInterval;
	}

	public void setCheckMemeberInterval(Integer checkMemeberInterval) {
		this.checkMemeberInterval = checkMemeberInterval;
	}
	
	
	
}
