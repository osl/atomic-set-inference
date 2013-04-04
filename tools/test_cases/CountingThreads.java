
public class CountingThreads extends Thread {

	private long max;
	private long val;

	public CountingThreads(long max) {
		this.max = max;
		this.val = 0;
	}
	
	public void run() {
		while (val < max) {
			++val;
		}
	}

	public static void main(String[] args) {
		CountingThreads c1 = new CountingThreads(1000);
		CountingThreads c2 = new CountingThreads(1000);
		c1.start();
		c2.start();
	}
	
}
