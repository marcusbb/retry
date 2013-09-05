package hazelcast;

import ies.retry.RetryCallback;
import ies.retry.RetryHolder;

public class FailSilentCallback implements RetryCallback {

		
		public boolean onEvent(RetryHolder holder) {
			
			
			//System.out.println("Retrying: "  + holder.getCount() + ":" + holder.getId()+":" +holder.getRetryData());
			
			return false;
		}
}
