package ies.retry.spi.hazelcast.persistence;

import ies.retry.RetryHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="RETRIES")
public class RetryEntity extends AbstractRetryEntity{

	private static final long serialVersionUID = -5185244774028196569L;

	/**
	 * Required constructor
	 */
	public RetryEntity() {
		
	}
	public RetryEntity(RetryHolder holder) throws IOException {
		List<RetryHolder> holders = new ArrayList<RetryHolder>(1);
		holders.add(holder);
		setHolderList(holders);
		populate(holders);
	}
	
	public RetryEntity(List<RetryHolder> holderList) throws IOException,ClassNotFoundException{
		setHolderList(holderList);
		populate(holderList);
	}

}
