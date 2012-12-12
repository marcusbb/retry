package ies.retry.spi.hazelcast.config;

import java.io.Serializable;

public class PubConfig implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3530342620303216572L;

	private boolean hzPub = true;
	
	//relavant for non hazelcast publication
	private int attempts = 3;

	//this may not be used yet
	private long pauseBeforeReattempt;
	
	//seconds to timeout the entire sync publish
	private long attemptTimeout = 10;
	
	public boolean isHzPub() {
		return hzPub;
	}

	public void setHzPub(boolean hzPub) {
		this.hzPub = hzPub;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public long getPauseBeforeReattempt() {
		return pauseBeforeReattempt;
	}

	public void setPauseBeforeReattempt(long pauseBeforeReattempt) {
		this.pauseBeforeReattempt = pauseBeforeReattempt;
	}

	public long getAttemptTimeout() {
		return attemptTimeout;
	}

	public void setAttemptTimeout(long attemptTimeout) {
		this.attemptTimeout = attemptTimeout;
	}
	
	
	
	
}
