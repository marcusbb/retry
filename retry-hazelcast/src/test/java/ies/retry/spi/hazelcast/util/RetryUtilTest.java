package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import ies.retry.RetryHolder;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class RetryUtilTest {

	private static long[][][] TIMESTAMPS = {
			{ { 1 }, { 1, 2, 3, 4 }, { 1, 2, 5 }, { 1, 2, 3 }, { 1, 2, 3 }, },

			{ { 1 }, { 1, 2, 3, 4 }, { 3, 4, 5 }, { 4, 5, 6, 7 }, { 1, 2 } } };

	private static int[] MERGED_SIZES = { 1, 4, 5, 7, 3 };

	@Test
	public void testMerge() throws Exception {
		for (int i = 0; i < MERGED_SIZES.length; i++) {
			List<RetryHolder> mergedList = RetryUtil.merge(createRetryList(0, i), createRetryList(1, i));
			assertEquals(MERGED_SIZES[i], mergedList.size());
			long prevTS = -1;
			for (int j = 0; j < mergedList.size(); j++) {
				long currTS = mergedList.get(j).getSystemTs();
				assertTrue("i=" + i + ", j=" + j + ", currTS=" + currTS
						+ ", prevTS=" + prevTS + ", " + mergedList, currTS > prevTS);
				
				prevTS = currTS;
			}

		}

	}

	private static List<RetryHolder> createRetryList(int listNumber,
			int position) {
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		for (int i = 0; i < TIMESTAMPS[listNumber][position].length; i++) {
			RetryHolder r = new RetryHolder("BB14", "CANADARM");
			r.setSystemTs(TIMESTAMPS[listNumber][position][i]);
			list.add(r);
		}
		return list;
	}
}
