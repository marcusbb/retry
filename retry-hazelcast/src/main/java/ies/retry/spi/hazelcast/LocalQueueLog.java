package ies.retry.spi.hazelcast;

import ies.retry.RetryHolder;
import ies.retry.spi.hazelcast.util.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import provision.services.logging.Logger;


/**
 * Commit (backup) log writer, which stores all objects in a single file commit log file
 * TODO: roll commit log file and take that into account on the marker logic
 * 
 * There are is additionally a state maintained, being the "take" marker, which is persisted
 * as well if the JVM is to crash. 
 * 
 * WorkFlow
 * <ol>
 * 	<li>{@link #queue(RetryHolder)} - stores the data
 * 	<li>{@link #moveTakeMarker()} - increments the take marker to the next point of where a potential write will occur
 * 	<li>{@link #replay()} - starting at the take marker will replay and return the objects from the last take marker
 * 	<li>{@link #moveTakeMarker()} - as client processes the collection, call to moveTakeMarker 
 * </ol>
 * 
 * Serialized commit queue writer, that adds and takes 
 * in a serialized way - requires an <strong>ordered</strong> take (poll) 
 * 
 * 
 * @author msimonsen
 *
 */
public class LocalQueueLog {

	String workingDir = ".";
	 FileOutputStream commitFout;
	 
	 //default commit log size
	 public static int DEF_MAX_COMMIT_LOG = 1024 * 1024 * 500;
	 private int commitLogSize = DEF_MAX_COMMIT_LOG;
	 
	 //rename this variable
	 public static String curFile = "commitlog";
	 public static String takeMarker = "take.marker";
	 RandomAccessFile takeOs;
	 Queue<Integer> writeQueue;
	 public static int INT_BYTE_SIZE = 4;
	
	 int curAddMarker = 0;
	 int curTakeMarker = 0;
	 //the system timestamp of a rolled file if any
	 long curTakeFile = 0;
	 
	public LocalQueueLog(String dirName) throws FileNotFoundException,IOException {
		this.workingDir = dirName;
		File dir = new File(dirName);
		if (!dir.exists())
			dir.mkdir();
		if (dir.isDirectory()) {

			File commitLog = new File(dir, curFile);
			if (commitLog.exists()) {
				// We don't commit log files that big > Integer.MAX_VALUE
				curAddMarker = (int) (commitLog.length() - 1);
			}
			commitFout = new FileOutputStream(commitLog, true);
			
			
		}
		File takeMarkerFile = new File(dir, takeMarker);
		takeOs = new RandomAccessFile(takeMarkerFile, "rw");

		byte[] b = new byte[4];
		takeOs.read(b);
		curTakeMarker = ByteBuffer.wrap(b).getInt();

		writeQueue = new LinkedList<Integer>();
		replayTakeQueue();
		
	}
	public LocalQueueLog(String dirName,int maxCommitLogSize) throws IOException,FileNotFoundException {
		this(dirName);
		this.commitLogSize = maxCommitLogSize;
	}
	public void queue(RetryHolder holder) throws IOException {
		
				
		byte [] b = IOUtil.serialize(holder);
		
		ByteBuffer intbb = ByteBuffer.allocate(INT_BYTE_SIZE);
		intbb.putInt(b.length);
		//write length and payload
		commitFout.write(intbb.array());
		commitFout.write(b);
		
		curAddMarker += b.length + INT_BYTE_SIZE ;
		writeQueue.add(b.length);
		
		
	}
	
	/**
	 * Increment the take marker - which will be called when client is processing the queue
	 * @throws IOException
	 */
	public void moveTakeMarker() throws IOException {
		
		curTakeMarker += writeQueue.poll() + INT_BYTE_SIZE;
		 
		writeTakeMarker();
		
		checkAndRollFile();
		
	}
	 
	private void writeTakeMarker() throws IOException {
		
		byte [] bInt = ByteBuffer.allocate(INT_BYTE_SIZE).putInt(curTakeMarker).array();
		//we may not write this all the time
		takeOs.write(bInt,0,INT_BYTE_SIZE);
			
					
		takeOs.seek(0);
	}
	public void close() throws IOException {
		commitFout.close();
		takeOs.close();
	}
	
	public Collection<RetryHolder> replay() throws IOException {
		return replayFromFile(false);
	}
	public Collection<RetryHolder> replay(boolean incrementTakeMarker) throws IOException {
		return replayFromFile(incrementTakeMarker);
	}
	
	private void replayTakeQueue() throws IOException {
		
		FileInputStream fin = new FileInputStream(new File(workingDir, curFile));
		
		fin.skip(curTakeMarker);
		 
		boolean EOF = false;
		byte []b = new byte[INT_BYTE_SIZE];
		//now loop over the file and serialize the objects
		while (!EOF) {
			
			if (fin.read(b) ==-1) 
				break;
			
			int nextBytes = ByteBuffer.wrap(b).getInt();
			byte []bObj = new byte[nextBytes];
			
			EOF = fin.read(bObj,0,nextBytes) == -1;
			if (nextBytes > 0)
				writeQueue.add(nextBytes);
		}
		fin.close();
	
	}
	/**
	 * Replay at the current take marker
	 * Will also increment the marker
	 *  Since this method is quite expensive, and will
	 *  only be executed when there is a "recovery" scenario
	 *  
	 *  If clients wish to increment the take marker you won't be able to 
	 *  replay it 
	 * @return
	 */
	protected Collection<RetryHolder> replayFromFile(boolean incrementTakeMarker) throws IOException {
		FileInputStream fin = null;
		Collection<RetryHolder> col = new ArrayList<RetryHolder>();
		
		try {
			fin = new FileInputStream(new File(workingDir, curFile));
		
			fin.skip(curTakeMarker);
			 
			boolean EOF = false;
			byte []b = new byte[INT_BYTE_SIZE];
			//now loop over the file and serialize the objects
			while (!EOF) {
				
				if (fin.read(b) ==-1) 
					break;
				
				int nextBytes = ByteBuffer.wrap(b).getInt();
				byte []bObj = new byte[nextBytes];
				
				EOF = fin.read(bObj,0,nextBytes) == -1;
				try {
					col.add( (RetryHolder)IOUtil.deserialize(bObj) );
				}catch (Exception e) {
					Logger.error(getClass().getName(), "QUEUE_SERIALIZATION","","msg",e.getMessage(),e);
				}
				if (incrementTakeMarker) {
					curTakeMarker += nextBytes +INT_BYTE_SIZE;
					
				}
			}
			
		}finally {
			if (incrementTakeMarker)
				writeTakeMarker();
			if (fin != null)
				fin.close();
		}
		return col;
	}
	
	
	public void removeFiles() throws IOException {
		close();
		
		File commitLog = new File(workingDir, curFile);
		commitLog.delete();
		//TODO: remove the rolled files
		File takeMarkerFile = new File(workingDir,takeMarker);
		takeMarkerFile.delete();
		
	}
	/**
	 * Written from the take marker - since it rolls the file
	 */
	private void checkAndRollFile() throws IOException {
		
		//should be curTakeMarker == curAddMarker under perfect conditions
		if (curTakeMarker > commitLogSize && curTakeMarker >= curAddMarker) {
			
			commitFout.close();
			File commitLog = new File(workingDir, curFile);
			commitLog.renameTo(new File(workingDir,curFile + "-" + System.currentTimeMillis()));
			commitFout = new FileOutputStream(commitLog, true);
			curAddMarker = 0;
			curTakeMarker = 0;
			//write out the take marker with current info:
			writeTakeMarker();
		}
	}
	
	
	protected File nextFile() {
		File f = new File(workingDir);
		String curName = curFile + "-" + curTakeFile;
		
		String [] names = f.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith(curFile))
					return true;
				return false;
			}
		});
		
		Arrays.sort(names);
		String retName = names[0];
		for (int i=0;i<names.length;i++) {
			if (curName.equals(names[i]) && i<names.length) {
				retName = names[i+1];
			}
		}
		//
		return new File(retName);
		
	}
	public int getTakeMarker() {
		return this.curTakeMarker;
	}
	public int getAddMarker() {
		return this.curAddMarker;
	}
}
