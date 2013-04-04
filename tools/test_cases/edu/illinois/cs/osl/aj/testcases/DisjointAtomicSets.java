package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.Address;
import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;

/**
 * A test program that generates a field access pattern of two atomic sets
 * from two disjoint units of work.  
 * 
 * Expected Atomic Sets
 * ====================
 * Address: A = {street, houseNumber}, B = {city, zip}
 * MyAddress: !czLock, !shLock
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class DisjointAtomicSets {
	
	private static MyAddress sharedAddress;

	// Subclass Address so we can define internal units of work.
	private static class MyAddress extends Address {
		private Object shLock;
		private Object czLock;

		public MyAddress(String street, int houseNumber, String city, int zip) {
			super(street, houseNumber, city, zip);
			shLock = new Object();
			czLock = new Object();
		}
		
		// The test case uses different locks, so sharedAddress is accessed
		// concurrently.  However, the access patterns are disjoint (do not overlap).
		public void setStreetHouseNumber(String s, int h) {
			synchronized(shLock) {
				this.setStreet(s);
				this.setHouseNumber(h);
			}
		}
		
		public void setCityZip(String c, int z) {
			synchronized(czLock) {
				this.setCity(c);
				this.setZip(z);
			}
		}
		
	}
	
	private static class WorkerThread extends IteratingThread {
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			sharedAddress.setStreetHouseNumber("Street" + tId, (int) tId);
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
