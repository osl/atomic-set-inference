package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that generates a field access pattern of an atomic
 * field that does _not_ alias an atomic set of its type.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName, lastName}
 * Proxy: P = {actualPerson}
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class UnaliasedFields {

	private static Proxy proxyA;
	private static Proxy proxyB;
	
	// Subclass Person so we can define an internal unit of work.
	private static class MyPerson extends Person {
		public MyPerson(String firstName, String lastName) {
			super(firstName, lastName);
		}
		
		// Access to firstName and lastName is always synchronized.
		synchronized public void setNames(String fn, String ln) {
			this.setFirstName(fn);
			this.setLastName(ln);
		}
	}

	private static class Proxy {
		private MyPerson actualPerson;
		
		public Proxy(MyPerson actualPerson) {
			this.actualPerson = actualPerson;
		}
		
		synchronized public MyPerson getActualPerson() {
			// This access to actualPerson and its fields is atomic,
			// which implies that the fields should be aliases of
			// actualPerson.
			//
			// However, the call to setNames(fn, ln) below is not
			// protected by any lock.  The resulting interleaving
			// prevents the aliasing.
			this.actualPerson.setNames("Foo", "Bar");
			return actualPerson;
		}
	}
	
	
	private static class WorkerThread extends IteratingThread {
		private Proxy proxy;
		
		public WorkerThread(Proxy p) {
			super(1000);
			this.proxy = p;
		}
		
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();
			MyPerson p = proxy.getActualPerson();
			p.setNames("Foo" + tId, "Bar" + tId);
		}
	}

	
	public static void main(String[] args) {
		MyPerson ap = new MyPerson("Foo", "Bar");
		proxyA = new Proxy(ap);
		proxyB = new Proxy(ap);
		
		WorkerThread threadA = new WorkerThread(proxyA);
		WorkerThread threadB = new WorkerThread(proxyB);
		
		threadA.start();
		threadB.start();
	}
	
}
