package ies.retry.spi.hazelcast;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A collection of statistics.
 * 
 * @author msimonsen
 *
 */
public class RetryStat implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 196432049453266665L;

	private String type;
	
	private AtomicLong totalSuccess = null;
	/**
	 * Each totalcount corresponds to the count index
	 */
	private Map<Integer,AtomicLong> totalFailed = null;

	private static long initTs  =-1;
	private long earliestTs = initTs; 
	
	
	public RetryStat() {
		//countIndex = new ArrayList<AtomicInteger>();
		 totalFailed = new HashMap<Integer,AtomicLong>();
	}
	
	public RetryStat(String type,int maxCount) {
		this.type = type;
		//countIndex = new ArrayList<AtomicInteger>(maxCount);
		reset(maxCount);
		this.totalSuccess = new AtomicLong(0);
	}
	
	private void reset(int maxCount) {
		totalFailed = new HashMap<Integer,AtomicLong>(maxCount);
		
		earliestTs = initTs;
	}
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
		

	/*public List<AtomicInteger> getCountIndex() {
		return countIndex;
	}

	public void setCountIndex(List<AtomicInteger> countIndex) {
		this.countIndex = countIndex;
	}*/

	
	public long getEarliestTs() {
		return earliestTs;
	}

	
	


	public AtomicLong getTotalSuccess() {
		return totalSuccess;
	}

	public void setTotalSuccess(AtomicLong totalSuccess) {
		this.totalSuccess = totalSuccess;
	}

	public Map<Integer, AtomicLong> getTotalFailed() {
		return totalFailed;
	}

	public void setTotalFailed(Map<Integer, AtomicLong> totalFailed) {
		this.totalFailed = totalFailed;
	}
	public void incrementFailed(Integer count) {
		if (totalFailed.get(count) == null ) {
			totalFailed.put(count, new AtomicLong(1));
		}else {
			totalFailed.get(count).incrementAndGet();
		}
	}
	public void setEarliestTs(long earliestTs) {
		this.earliestTs = earliestTs;
	}
	public void setIfEarlier(long ts) {
		if (earliestTs == initTs)
			this.earliestTs = ts;
		else if (ts < earliestTs)
			this.earliestTs = ts;
	}
	
	public void resetFailed() {
		reset(totalFailed.size());
	}

	@Override
	public String toString() {
		Date ts = earliestTs == initTs?null:new Date(earliestTs);
		return "RetryStat [type=" + type + ", totalSuccess=" + totalSuccess
				+ ", totalFailed=" + totalFailed + ", earliestTs=" + ts
				+ "]";
	}

	public String totalFailedtoString() {
		StringBuilder builder = new StringBuilder();
		
		return builder.toString();
	}
	

	
	
}
