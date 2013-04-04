package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.Address;
import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;

/**
 * A test program that generates a field access pattern of a single atomic set
 * from two overlapping (internal) units of work.  
 * 
 * Expected Atomic Sets
 * ====================
 * Address: A = {street, city, zip}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class OverlappingAtomicSets {
	
	private static MyAddress sharedAddress;

	// Subclass Address so we can define an internal unit of work.
	private static class MyAddress extends Address {

		public MyAddress(String street, int houseNumber, String city, int zip) {
			super(street, houseNumber, city, zip);
		}
		
		// Both methods access the city field atomically and _protected by the
		// SAME lock_!  Thus, the atomic sets {street, city} and {city, zip}
		// should be merged.
		//
		// Using different locks leads to concurrent access to city.
		//
		// NOTE: Using a synchronized block with an explicit lock object
		//       can lead to (technically correct) overlapping of access
		//       to city when one thread has exited the block, but not yet
		//       the method, and the other enters the block.
		synchronized public void setStreetCity(String s, String c) {
			this.setStreet(s);
			this.setCity(c);
		}
		
		synchronized public void setCityZip(String c, int z) {
			this.setCity(c);
			this.setZip(z);
		}
		
	}
	
	private static class WorkerThread extends IteratingThread {
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			sharedAddress.setStreetCity("Street" + tId, "City" + tId);
			sharedAddress.setCityZip("City" + tId, (int) tId);
		}
	}
	
	
	public static void main(String[] args) {
		sharedAddress = new MyAddress("Street", 0, "City", 0);
		
		WorkerThread threadA = new WorkerThread();
		WorkerThread threadB = new WorkerThread();
		
		threadA.start();
		threadB.start();
	}
	
}
