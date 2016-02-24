package ies.retry.spi.hazelcast.persistence;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HzIntegrationTestUtil;
import ies.retry.spi.hazelcast.disttasks.DistCallBackTaskSyncTest;
import ies.retry.spi.hazelcast.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TemporalType;

import org.hibernate.lob.SerializableBlob;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class ArchivingTest {
	private static org.slf4j.Logger logger =  org.slf4j.LoggerFactory.getLogger(ArchivingTest.class);
	private static EntityManagerFactory emf;
	private static String TYPE = "TEST_ARCHIVED_1";
	String id = "ID_" + System.currentTimeMillis();

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		HzIntegrationTestUtil.beforeClass();
		emf = Persistence.createEntityManagerFactory("retryPool");
	}

	@AfterClass
	public static void setUpAfterClass() throws Throwable {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createNativeQuery("delete from RETRIES_ARCHIVE where NATURAL_IDENTIFIER='"+ TYPE+ "'").executeUpdate();
		em.getTransaction().commit();
		emf.close();
		HzIntegrationTestUtil.afterClass();
	}

	@Test
	public void testInserts() {

		ArrayList<RetryHolder> holders = new ArrayList<RetryHolder>();
		List<RetryHolder> holdersCopy = null;
		EntityManager em = null;
		try {
			em = emf.createEntityManager();

			em.getTransaction().begin();

			for (int i = 0; i < 10; i++) {
				RetryHolder holder = new RetryHolder(id, TYPE);
				holder.setException(new Exception("Something is really wrong"));
				holder.setCount(i);
				holder.setNextAttempt(System.currentTimeMillis() + 10000);
				holder.setSystemTs(System.currentTimeMillis());
				holder.setRetryData("It's interesting to note that you're not limited to JPQL when defining queries to be then executed with Query API. You may be surprised to learn that the EntityManager API offers methods for creating instances of Query for executing native SQL statements. The most important thing to understand about native SQL queries created with EntityManager methods is that they, like JPQL queries, return entity instances, rather than database table records. Here is a simple example of a dynamic native SQL query");
				holders.add(holder);
			}

			int i = 1;
			em.createNativeQuery(
					"INSERT INTO RETRIES_ARCHIVE (RETRY_TYPE, NATURAL_IDENTIFIER, RETRY_VER, PAYLOAD_DATA, ORIGINAL_TS) "
							+ "       VALUES(?, ?, ?, ?, ?)")
					.setParameter(i++, TYPE)
					.setParameter(i++, id)
					.setParameter(i++, 5)
					.setParameter(i++, IOUtil.serialize(holders))
					.setParameter(i++,
							new Timestamp(System.currentTimeMillis()),
							TemporalType.TIMESTAMP).executeUpdate();
			em.flush();

			em.getTransaction().commit();
			em.close();
			em = emf.createEntityManager();
			em.getTransaction().begin();
			SerializableBlob blob = (SerializableBlob) em.createNativeQuery(
					"SELECT PAYLOAD_DATA FROM RETRIES_ARCHIVE where retry_type='"
							+ TYPE + "' and natural_identifier='" + id + "'")
					.getSingleResult();

			ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
			InputStream is = blob.getBinaryStream();
			int count = 0;
			int len = 1024;
			byte[] buff = new byte[len];
			while ((count = is.read(buff, 0, len)) > -1) {
				bos.write(buff, 0, count);
			}

			holdersCopy = (List<RetryHolder>) IOUtil.deserialize(bos
					.toByteArray());
		} catch (Exception e) {
			//em.getTransaction().rollback();
			e.printStackTrace();
			logger.error("Persistence_Op_Exception Exception Message: {}" , e.getMessage(), e);
		} finally {
			if (em != null)
				em.close();
		}

		assertNotNull(holdersCopy);
		assertEquals(holders.size(), holdersCopy.size());
	}

}
