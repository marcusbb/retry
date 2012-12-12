package ies.retry.spi.hazelcast;

import java.util.HashMap;
import java.util.Map;

/**
 * Retry stats mapped by type.
 * 
 * @author msimonsen
 *
 */
public class RetryStats {

	/**
	 * Mapped by type
	 */
	private Map<String,RetryStat> allStats;
	
	public RetryStats() {
		allStats = new HashMap<String, RetryStat>();
	}

	public Map<String, RetryStat> getAllStats() {
		return allStats;
	}

	public void setAllStats(Map<String, RetryStat> allStats) {
		this.allStats = allStats;
	}
	
	public void put(String type,RetryStat stat) {
		allStats.put(type, stat);
	}
	public void removeAll() {
		allStats.clear();
	}

	@Override
	public String toString() {
		StringBuilder builder= new StringBuilder("RetryStats [allStats=\n");
		  
		for (RetryStat stat: allStats.values()) {
			builder.append("\t"+ stat.toString() + "\n");
		}
		builder.append("]");
		return builder.toString();
	}
	
	
}
