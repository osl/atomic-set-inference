package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program with a method that performs an atomic action on
 * a combination of its input parameters, making the method an external
 * unit of work for the parameters.  The action is invoked via a wrapper
 * method that has no effects on its own and should consequently _not_ be
 * a unit of work for the parameters.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName}
 *
 * Expected External Units of Work
 * ===============================
 * atomicSwap: p1=N, p2=N
 * swapIdentities: --
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class IndirectExternalUnitOfWork {

	private static class SecretService {
		static private Object lock = new Object();

		private static void atomicSwap(Person p1, Person p2) {
			// This method is an external unit of work for both p1 and p2.
			synchronized(lock) {
				String fn = p1.getFirstName();
				p1.setFirstName(p2.getFirstName());
				p2.setFirstName(fn);
			}
		}

		public static void swapIdentities(Person p1, Person p2) {
			// This method only redirects the access and has no
			// actual effect on p1 and p2.  Thus, it should _not_ be
			// a unit of work.
			atomicSwap(p1, p2);
		}
	}
	
	private static class WorkerThread extends IteratingThread {
		Person p1;
		Person p2;
		
		public WorkerThread(Person p1, Person p2) {
			this.p1 = p1;
			this.p2 = p2;
		}
		
		protected void doWork(int iteration) {
			SecretService.swapIdentities(p1, p2);
		}
	}

	
	public static void main(String[] args) {
		Person p1 = new Person("Foo", "Bar");
		Person p2 = new Person("Baz", "Boo");
		
		WorkerThread threadA = new WorkerThread(p1, p2);
		WorkerThread threadB = new WorkerThread(p1, p2);
		
		threadA.start();
		threadB.start();
	}
	
}
