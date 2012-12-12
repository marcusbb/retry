package ies.retry.spi.hazelcast.query;

import ies.retry.RetryHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.hazelcast.core.Member;

public class QueryResults implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4576925038695165980L;

	private Collection<RetryHolder> results;
	
	private Member member;
	
	public QueryResults() {}
	
	public QueryResults(Collection<RetryHolder> results) {
		this.results = results;
	}

	public Collection<RetryHolder> getResults() {
		return results;
	}

	public void setResults(Collection<RetryHolder> results) {
		this.results = results;
	}
	public void add(RetryHolder holder) {
		if (results == null)
			results = new ArrayList<RetryHolder>();
		results.add(holder);
	}
	public void add(Collection<RetryHolder> list) {
		if (results == null)
			results = new ArrayList<RetryHolder>();
		results.addAll(list);
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}
	
}
