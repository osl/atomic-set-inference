package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.Address;
import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that generates a field access pattern which includes
 * aliased atomic sets from child objects.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName, lastName}
 * Address: A = {street, houseNumber}
 * Customer: C = {person=N, address=A}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class AliasedFields {
	
	private static Customer sharedCustomer;

	private static class Customer {
		private Person person;
		private Address address;
		
		public Customer() {
			this.person = new Person("Foo", "Bar");
			this.address = new Address("Street", 0, "City", 0);
		}

		// All field accesses are atomic.
		synchronized public void update(String fn, String ln, String s, int h) {
			person.setFirstName(fn);
			person.setLastName(ln);
			address.setStreet(s);
			address.setHouseNumber(h);
		}
	}
	
	
	private static class WorkerThread extends IteratingThread {
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			sharedCustomer.update("First" + tId, "Last" + tId, "Street" + tId, (int) tId);
		}
	}

	
	public static void main(String[] args) {
		sharedCustomer = new Customer();
		
		WorkerThread threadA = new WorkerThread();
		WorkerThread threadB = new WorkerThread();
		
		threadA.start();
		threadB.start();
	}
	
}
