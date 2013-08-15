package ies.retry.spi.hazelcast;

import ies.retry.Retry;
import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.query.BeanShellQuery;
import ies.retry.spi.hazelcast.query.PropertyQuery;
import ies.retry.spi.hazelcast.query.QueryResults;
import ies.retry.spi.hazelcast.query.RetryQueryUtil;
import ies.retry.spi.hazelcast.query.SampleQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.SqlPredicate;

public class QueryTest {

	private static HazelcastInstance h1 = null;
	private static IMap<String,List<RetryHolder>> map;
	
	@BeforeClass
	public static void beforeClass() {
		HazelcastRetryImpl retryManager = ((HazelcastRetryImpl)Retry.getRetryManager());
		h1 = retryManager.getHzInst();
		
		map = h1.getMap("retry");
		Employee emp = new Employee("marcus", 30, "developer");
		ArrayList<RetryHolder> list = new ArrayList<RetryHolder>();list.add(new RetryHolder("123", "retry",new Exception(),emp));
		map.put("123",list );
		
		Employee emp2 = new Employee("imtiyaz", 25, "developer");
		list = new ArrayList<RetryHolder>();list.add(new RetryHolder("1234", "retry",new Exception(),emp2));
		map.put("1234", list);
		
		Employee emp3 = new Employee("tran", 25, "developer");
		list = new ArrayList<RetryHolder>();list.add(new RetryHolder("1235", "retry",new Exception(),emp3));
		map.put("1235", list);
		
		
	}
	@AfterClass
	public static void afterClass() {
		((HazelcastRetryImpl)Retry.getRetryManager()).shutdown();
	}
	//@Test
	public void addAndQuery() {
		
		
		Collection values = map.values(new SqlPredicate("type = 'retry' "));
		
		EntryObject e = new PredicateBuilder().getEntryObject();
		
	}
	
	@Test
	public void testSampleQuery() {
		SampleQuery query = new SampleQuery("retry", 0, 1);
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(1, results.getResults().size());
		
		
	}
	@Test
	public void testSampleMaxEqualsQuery() {
		SampleQuery query = new SampleQuery("retry", 0, 3);
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(3, results.getResults().size());
		
		
	}
	@Test
	public void testSampleOutofbounds() {
		SampleQuery query = new SampleQuery("retry", 0, 10);
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(3, results.getResults().size());
		
		
	}
	
	@Test
	public void propertyEquals() {
		PropertyQuery query = new PropertyQuery("retry", "name","marcus");
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(1, results.getResults().size());
		System.out.println("Employee: " + results.getResults().iterator().next().getRetryData());
		RetryHolder expected = map.get("123").get(0);
		System.out.println("Employee: " + expected.getRetryData());
		Assert.assertEquals(expected, results.getResults().iterator().next());
		
		
	}
	
	@Test
	public void beanShellTest() {
		BeanShellQuery query = new BeanShellQuery("retry", "data.getAge() > 10");
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(3, results.getResults().size());
		
		
	}
	@Test
	public void beanShellTest2() {
		BeanShellQuery query = new BeanShellQuery("retry", "retry.getCount() > 1");
		QueryResults results= RetryQueryUtil.execute(query);
		Assert.assertEquals(0, results.getResults().size());
		
		
	}
	
}
