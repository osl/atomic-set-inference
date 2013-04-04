package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that generates a field access pattern of a single atomic set
 * and a non-atomic field.  
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName}, !lastName
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class NonAtomicField {

	private static MyPerson sharedPerson;

	// Subclass Person so we can define an internal unit of work.
	private static class MyPerson extends Person {
		public MyPerson(String firstName, String lastName) {
			super(firstName, lastName);
		}
		
		// Access to firstName and lastName is always synchronized.
		// However, there is an unsynchronized access to lastName below.
		synchronized public void setNames(String fn, String ln) {
			this.setFirstName(fn);
			this.setLastName(ln);
		}
	}
	
	
	private static class WorkerThread extends IteratingThread {
		// Access to the last name is unsynchronized.  Even a single
		// read can generate interleaving witnesses, but we do several
		// accesses to make this more reliable.
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			for (int i=0; i<10; ++i) {
				sharedPerson.setLastName("Last" + tId);
			}
			sharedPerson.setNames("First" + tId, "Last" + tId);
		}
	}

	
	public static void main(String[] args) {
		sharedPerson = new MyPerson("Foo", "Bar");
		
		WorkerThread threadA = new WorkerThread();
		WorkerThread threadB = new WorkerThread();
		
		threadA.start();
		threadB.start();
	}

}
