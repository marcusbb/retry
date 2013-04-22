package ies.retry.spi.hazelcast.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import ies.retry.RetryHolder;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class RetryUtilTest {

	// test input
	private static long[][][] TIMESTAMPS_REGULAR_MERGE = {
			{ { 2 }, { 2, 4, 6, 8 }, { 2, 4, 10 }, { 2, 4, 6 }, { 2, 4, 6 }, },

			{ { 2 }, { 2, 4, 6, 8 }, { 3, 5, 10 }, { 3, 5, 7, 9 }, { 2, 4 } } };

	// test result
	private static int[] MERGED_SIZES = { 1, 4, 5, 7, 3 };

	// This test verifies generic merge used to merge together master/local/db copies of retries 
	@Test
	public void testMerge() throws Exception {
		for (int i = 0; i < MERGED_SIZES.length; i++) {
			List<RetryHolder> firstList =  createRetryList(TIMESTAMPS_REGULAR_MERGE, 0, i, 10);
			List<RetryHolder> secondList = createRetryList(TIMESTAMPS_REGULAR_MERGE, 1, i, 0);
			List<RetryHolder> mergedList = RetryUtil.merge(firstList, secondList);
			assertEquals(MERGED_SIZES[i], mergedList.size());
			long prevTS = -1;
			for (int j = 0; j < mergedList.size(); j++) {
				long currTS = mergedList.get(j).getSystemTs();
				assertTrue("i=" + i + ", j=" + j + ", currTS=" + currTS
						+ ", prevTS=" + prevTS + ", " + mergedList, currTS > prevTS);
				prevTS = currTS;
				
				if(currTS%2==0)// item is from the first list
					Assert.assertEquals(10,  mergedList.get(j).getCount());
			}

		}

	}

	private static List<RetryHolder> createRetryList(long[][][] tsArray, int listNumber,
			int position, int retryCount) {
		List<RetryHolder> list = new ArrayList<RetryHolder>();
		for (int i = 0; i < tsArray[listNumber][position].length; i++) {
			RetryHolder r = new RetryHolder("BB14", "CANADARM");
			r.setSystemTs(tsArray[listNumber][position][i]);
			r.setCount(retryCount);
			list.add(r);
		}
		return list;
	}
	
	
	// test input
	private static long[][][] TIMESTAMPS_DISTCALLBACK_MERGE = {
		{ { 1, 2, 3, 4 },     { 1, 2, 3, 4 }, { 1, 2, 3, 4 },    { 1, 2, 3, 4 }, { 1, 2, 3, 4 },  },

		{ {  3, 4 },          { 1, 2, 3, 4 }, { 1, 2, 3, 4 },    {},             { },          }, 

		{ { 1, 2, 3, 4, 11 },  { 1, 2, 3, 4 }, { 1, 2, 3, 4, 11 }, { 1, 2, 3, 4 }, { 1, 2, 3, 4, 11, 12} } 
		
	};
	

	// test resut
	private static int[][] TIMESTAMPS_DISTCALLBACK_MERGE_RESULT = { {3, 4, 11}, { 1, 2, 3, 4 }, { 1, 2, 3, 4, 11}, {}, {11, 12} };
	
	//merge(String caller, List<RetryHolder> original, List<RetryHolder> failed, List<RetryHolder> latest)
	
	
	// Tests merge of failed, local and master list in DistCallBackTask
	@Test
	public void testMergeDistCallback() throws Exception {
		for (int i = 0; i < TIMESTAMPS_DISTCALLBACK_MERGE_RESULT.length; i++) {
			List<RetryHolder> mergedList = RetryUtil.merge("RetryUtil", createRetryList(TIMESTAMPS_DISTCALLBACK_MERGE, 0, i, 0), 
					createRetryList(TIMESTAMPS_DISTCALLBACK_MERGE, 1, i, 10), createRetryList(TIMESTAMPS_DISTCALLBACK_MERGE, 2, i, 0));

			assertEquals(TIMESTAMPS_DISTCALLBACK_MERGE_RESULT[i].length, mergedList.size());
			long prevTS = -1;
			for (int j = 0; j < mergedList.size(); j++) {
				long currTS = mergedList.get(j).getSystemTs();
				long expectedTS = TIMESTAMPS_DISTCALLBACK_MERGE_RESULT[i][j];
				assertEquals("i=" + i + ", j=" + j + ", currTS=" + currTS
						+ ", prevTS=" + prevTS + ", " + mergedList, currTS, expectedTS);
				
				prevTS = currTS;
				if(currTS<10)// item is from the first list
					Assert.assertEquals(10,  mergedList.get(j).getCount());
			}

		}

	}

}
