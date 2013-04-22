package ies.retry.spi.hazelcast.persistence.ops;

import java.sql.Timestamp;
import java.util.List;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TemporalType;

public class ArchiveOp  extends DelOp{
	private final static String CALLER = ArchiveOp.class.getName();
	private boolean removeEntity;

	public ArchiveOp(EntityManagerFactory emf, List<RetryHolder> list, boolean removeEntity){		
		super(emf, list.get(0).getType(), list.get(0).getId());
		this.removeEntity = removeEntity;
		setListHolder(list);
	}
	
	@Override
	public Void exec(EntityManager em) throws Exception {

		List<RetryHolder> holders = getListHolder();
		
		RetryHolder holder = holders.get(0);
		int i = 1;
		em.createNativeQuery(
				"INSERT INTO RETRIES_ARCHIVE (RETRY_TYPE, NATURAL_IDENTIFIER, RETRY_VER, PAYLOAD_DATA, ORIGINAL_TS) "
						+ "       VALUES(?, ?, ?, ?, ?)")
				.setParameter(i++, holder.getType())
				.setParameter(i++, holder.getId())
				.setParameter(i++, holder.getCount())
				.setParameter(i++, IOUtil.serialize(holders))
				.setParameter(i++,
						new Timestamp(holder.getSystemTs()),
						TemporalType.TIMESTAMP).executeUpdate();

		if(removeEntity) // delete original entity if the whole list expired
			super.exec(em);
		return null;
	}

}
