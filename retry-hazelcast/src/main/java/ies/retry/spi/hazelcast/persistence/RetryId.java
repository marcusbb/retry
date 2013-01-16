package ies.retry.spi.hazelcast.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class RetryId implements Serializable{
	
	/**
	 * generated
	 */
	private static final long serialVersionUID = -251304715190051339L;


	public RetryId() {}
	public RetryId(String id,String type) {
		this.id=  id;
		this.type = type;
		
	}
	@Column(name="NATURAL_IDENTIFIER")
	private String id;
	
	@Column(name="RETRY_TYPE")
	private String type;
	
	

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		RetryId retryid = (RetryId)obj;
		return (id.equals(retryid.getId()) && type.equals(retryid.getType()));
	}
	
}
