package ies.retry.spi.hazelcast.disttasks;

import ies.retry.RetryState;

import java.io.Serializable;

import com.hazelcast.core.Member;

public class BroadcastState implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2641003248865190040L;
	private RetryState retryState;
	private Member member;
	
	public BroadcastState(RetryState retryState,Member member) {
		this.retryState = retryState;
		this.member = member;
	}

	public RetryState getRetryState() {
		return retryState;
	}

	public void setRetryState(RetryState retryState) {
		this.retryState = retryState;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}
	
	
}
