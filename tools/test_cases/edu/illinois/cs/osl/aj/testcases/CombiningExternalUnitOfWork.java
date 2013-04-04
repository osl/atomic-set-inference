package edu.illinois.cs.osl.aj.testcases;

import edu.illinois.cs.osl.aj.testcases.common.IteratingThread;
import edu.illinois.cs.osl.aj.testcases.common.Person;

/**
 * A test program with a method that performs an atomic action on
 * a combination of its input parameters, making the method an external
 * unit of work for the parameters.  The action is invoked via a wrapper
 * method.  Unlike in the IndirectExternalUnitOfWork case, the wrapper
 * has an additional effect, making it a unit of work in its own right.
 * 
 * Expected Atomic Sets
 * ====================
 * Person: N = {firstName}
 * WorkerThread: P = {p1, p2}
 *
 * Expected External Units of Work
 * ===============================
 * atomicSwap: p1=N, p2=N
 * swapIdentities: p1=N, p2=N
 * 
 * @author Peter Dinges <pdinges@acm.org>
 */
public class CombiningExternalUnitOfWork {

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
			// The method combines the effects of two units of work
			// and should thus also be a unit of work for the involved
			// (both, p1 and p2) input parameters.
			synchronized(lock) {
				atomicSwap(p1, p2);
				p1.getFirstName();
			}
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
