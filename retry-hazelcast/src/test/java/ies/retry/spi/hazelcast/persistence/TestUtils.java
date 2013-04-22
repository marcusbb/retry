package ies.retry.spi.hazelcast.persistence;


import javax.persistence.EntityManager;
import javax.persistence.Query;

public class TestUtils {
	
	public static int getNumberOfRows(EntityManager em, boolean archive, String type, String id){
		Query query = em
				.createNativeQuery("SELECT count(*) FROM RETRIES" + (archive ? "_ARCHIVE" : "") + " where retry_type='" + type + "' and NATURAL_IDENTIFIER='"+ id+"'");
		Number countResult = (Number) query.getSingleResult();
		return countResult.intValue();
	}

}
