package ies.retry.spi.hazelcast.disttasks;

import java.io.Serializable;

import com.hazelcast.core.Member;

public class CallbackRegistration implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6561971615178162380L;
	private Member member;
	private boolean registered = true;
	
	public CallbackRegistration(Member member,boolean registered) {
		this.member = member;
		this.registered = registered;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}
	
}
