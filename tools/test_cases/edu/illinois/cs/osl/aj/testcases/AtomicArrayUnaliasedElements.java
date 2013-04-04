package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program that atomically accesses an array, while accessing some of
 * the array elements in an interleaved manner (which prevents aliasing
 * atomic sets of the elements).
 * 
 * Expected Atomic Sets
 * ====================
 * PersonDB: E = {entries={entries[]}},
 * Person: N = !firstName
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class AtomicArrayUnaliasedElements {

	private static class PersonDB {
		private Person[] entries;
		
		public PersonDB(Person[] entries) {
			this.entries = entries;
		}
		
		synchronized public Person get(int index) {
			// This method atomically accesses the entries array, as well as
			// the firstName field of the selected Person.  This suggests the
			// alias entries={entries[]={firstName}}.
			Person p = entries[index];
			p.getFirstName();
			return p;
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		private PersonDB database;
		
		public WorkerThread(PersonDB db) {
			super(1000);
			this.database = db;
		}
		
		protected void doWork(int iteration) {
			long tId = Thread.currentThread().getId();

			Person p1 = database.get(0);
			Person p2 = database.get(1);

			// Access to the first name is not protected by a lock.
			// The interleaved access cancels the aliasing intentions
			// of Person.get() .
			p1.setFirstName("Foo" + tId);
			p2.setFirstName("Bar" + tId);
		}
	}

	
	public static void main(String[] args) {
		Person p1 = new Person("Foo", "Bar");
		Person p2 = new Person("Baz", "Boo");
		PersonDB db = new PersonDB(new Person[] {p1, p2});
		
		WorkerThread threadA = new WorkerThread(db);
		WorkerThread threadB = new WorkerThread(db);
		
		threadA.start();
		threadB.start();
	}
	
}
