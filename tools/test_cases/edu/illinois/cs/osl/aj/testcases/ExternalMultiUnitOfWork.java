package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.Address;
import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;

/**
 * A test program with a method that performs an atomic action on
 * a combination of its input parameters, making the method an external
 * unit of work for the parameters.  However, the parameters have multiple
 * atomic sets, all of which are used by the external unit of work.
 * 
 * Expected Atomic Sets
 * ====================
 * Address: S = {street, houseNumber}, C = {city, zip}
 *
 * Expected External Units of Work
 * ===============================
 * compare: a1=S+C, a2=S+C
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class ExternalMultiUnitOfWork {

	private static class AddressComparator {
		public static boolean compare(Address a1, Address a2) {
			// This method computes nonsense, but ensures that all methods are
			// actually called.  (Avoid using connectors with short-circuit
			// semantics like && and ||).
			boolean tmp;
			tmp = a1.getStreet() == a2.getStreet();
			tmp = a1.getHouseNumber() == a2.getHouseNumber();
			tmp = a1.getCity() == a2.getCity();
			tmp = a1.getZip() == a2.getZip();
			return tmp;
		}
	}

	// Subclass Address so we can define internal units of work.
	private static class MyAddress extends Address {
		public MyAddress(String street, int houseNumber, String city, int zip) {
			super(street, houseNumber, city, zip);
		}
		
		// The test case uses different locks, so the address is accessed
		// concurrently.  However, the access patterns are disjoint
		// (do not overlap).
		synchronized public void setStreetHouseNumber(String s, int h) {
				this.setStreet(s);
				this.setHouseNumber(h);
		}
		
		synchronized public void setCityZip(String c, int z) {
				this.setCity(c);
				this.setZip(z);
		}
		
	}

	private static class WorkerThread extends IteratingThread {
		MyAddress a1;
		MyAddress a2;
		
		public WorkerThread(MyAddress a1, MyAddress a2) {
			this.a1 = a1;
			this.a2 = a2;
		}
		
		protected void doWork(int iteration) {
			// Ensure that at least one internal atomic set exists.
			a1.setStreetHouseNumber("Street", 0);
			a1.setCityZip("City", 0);
			synchronized(a1) {
				AddressComparator.compare(a1, a2);
			}
		}
	}

	
	public static void main(String[] args) {
		MyAddress a1 = new MyAddress("Street", 0, "City", 0);
		MyAddress a2 = new MyAddress("Avenue", 0, "Metropolis", 0);

		// Generate the two atomic sets in a1
		a1.setStreetHouseNumber("Street", 0);
		a1.setCityZip("City", 0);

		WorkerThread threadA = new WorkerThread(a1, a2);
		WorkerThread threadB = new WorkerThread(a1, a2);
		
		threadA.start();
		threadB.start();
	}

}
