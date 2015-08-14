package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.HzSerializableRetryHolder;
import ies.retry.spi.hazelcast.util.HzUtil;
import ies.retry.spi.hazelcast.util.KryoSerializer;

import org.junit.Test;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class SerializerTest {

	@Test
	public void test() {
		Config hzConfig = new ClasspathXmlConfig(HzUtil.HZ_CONFIG_FILE);
		HazelcastInstance hzInst = Hazelcast.newHazelcastInstance(hzConfig);
		
		IMap<String,List<RetryHolder>> map = hzInst.getMap("serializer-test");
		
		ArrayList<RetryHolder> holderList = new ArrayList<>();
		holderList.add(new RetryHolder("id1", "type1", new HashMap<>()));
	
		map.put("id1", new HzSerializableRetryHolder(holderList, new KryoSerializer()));
		
		List<RetryHolder> getList = map.get("id1");
		
		assertNotNull(getList);
		
		assertEquals(1,getList.size());
		hzInst.getLifecycleService().shutdown();
	}
	@Test
	public void testMultiMap() {
		Config hzConfig = new ClasspathXmlConfig(HzUtil.HZ_CONFIG_FILE);
		HazelcastInstance hzInst = Hazelcast.newHazelcastInstance(hzConfig);
		
		IMap<String,List<RetryHolder>> map = hzInst.getMap("serializer-test");
		
		ArrayList<RetryHolder> holderList = new ArrayList<>();
		holderList.add(new RetryHolder("id1", "type1", new HashMap<>()));
		
		map.put("id1", new HzSerializableRetryHolder(holderList, new KryoSerializer()));
		
		List<RetryHolder> getList = map.get("id1");
				
		assertNotNull(getList);
		
		assertEquals(1,getList.size());
		
		assertTrue(getList instanceof HzSerializableRetryHolder);
		
		getList.add(new RetryHolder("id1", "type1", new HashMap<>()));
		map.put("id1", getList);
		
		assertEquals(2,map.get("id1").size());
		hzInst.getLifecycleService().shutdown();
	}

}
