package ies.retry.spi.hazelcast.persistence;

import ies.retry.RetryHolder;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="RETRIES_ARCHIVE")
public class ArchivedRetryEntity  extends AbstractRetryEntity {

	private static final long serialVersionUID = -6035429976705515451L;
	
	@Column(name = "ORIGINAL_TS")
	private Timestamp originalTS;

	public Timestamp getOriginalTS() {
		return originalTS;
	}

	public void setOriginalTS(Timestamp originalTS) {
		this.originalTS = originalTS;
	}

	/**
	 * Required constructor
	 */
	public ArchivedRetryEntity() {
	}
	

	public ArchivedRetryEntity(List<RetryHolder> holderList) throws IOException,ClassNotFoundException{
		setHolderList(holderList);
		populate(holderList);
		this.originalTS = new Timestamp(holderList.get(0).getSystemTs());
	}
}

	
