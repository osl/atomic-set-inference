
public class FieldAccess {

	private static class Point {
		int x, y;
		boolean changed;
		
		public Point(int x, int y) { this.x = x; this.y = y; }
		public int getX() { return x; }
		public int getY() { return y; }
		public void setX(int x) { this.x = x; this.changed = true; }
		public void setY(int y) { this.y = y; this.changed = true; }
	}

	static void singleSet(Point p) {
		p.setX(23);
	}
	
	static void doubleGet(Point p) {
		// Unit of work for p.x and p.y
		int u = p.getX();
		int v = p.getY();
	}
	
	
	public static void main(String[] args) {
		Point p = new Point(1, 2);
		Object lock = new Object();
		WorkerThread threadA = new WorkerThread(p, lock);
		WorkerThread threadB = new WorkerThread(p, lock);
		
		threadA.start();
		threadB.start();
	}
	
	private static class WorkerThread extends Thread {
		private Point sharedPoint;
		private Object lock;
		
		public WorkerThread(Point p, Object lock) {
			this.sharedPoint = p;
			this.lock = lock;
		}
		
		public void run() {
			for (int i=0; i < 1000; ++i) {
				synchronized(lock) {
					doubleGet(this.sharedPoint);
				}
			}
		}
	}

}
