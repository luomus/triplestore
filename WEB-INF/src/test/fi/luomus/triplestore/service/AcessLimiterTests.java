package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import fi.luomus.triplestore.utils.AccessLimiter;
import fi.luomus.triplestore.utils.AccessLimiter.Access;
import fi.luomus.triplestore.utils.AccessLimiter.TooManyRequestsException;

public class AcessLimiterTests {

	@Test
	public void test() throws TooManyRequestsException {
		AccessLimiter limiter = new AccessLimiter(2);
		Access a1 = limiter.acquire("a");
		limiter.acquire("a");
		limiter.acquire("b");
		a1.release();
		limiter.acquire("a");
		try {
			limiter.acquire("a");
			fail("Should throw exception");
		} catch (TooManyRequestsException e) {
			assertEquals("Too many pending requests from user 'a'. Max allowed: 2", e.getMessage());
		}
	}

	@Test
	@Ignore
	public void tes2t() throws InterruptedException {
		final AccessLimiter limiter = new AccessLimiter(2);
		Thread t1 = new Thread(new Runnable() {
			int i = 0;
			@Override
			public void run() {
				while (true) {
					try {
						Access access = limiter.acquire("a");
						System.out.println("T1 got access " + i++);
						try { Thread.sleep(3000); } catch (InterruptedException e) {}
						access.release();
					} catch (TooManyRequestsException e1) {
						System.out.println("T1 " + e1);
					}
					try { Thread.sleep(200); } catch (InterruptedException e) {}
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			int i = 0;
			@Override
			public void run() {
				while (true) {
					try {
						Access access = limiter.acquire("a");
						System.out.println("T2 got access " + i++);
						try { Thread.sleep(8000); } catch (InterruptedException e) {}
						access.release();
					} catch (TooManyRequestsException e1) {
						System.out.println("T2 " + e1);
					}
					try { Thread.sleep(200); } catch (InterruptedException e) {}
				}
			}
		});
		Thread t3 = new Thread(new Runnable() {
			int i = 0;
			@Override
			public void run() {
				while (true) {
					try {
						Access access = limiter.acquire("a");
						System.out.println("T3 got access " + i++);
						try { Thread.sleep(50); } catch (InterruptedException e) {}
						access.release();
					} catch (TooManyRequestsException e1) {
						System.out.println("T3 " + e1);
					}
					try { Thread.sleep(200); } catch (InterruptedException e) {}
				}
			}
		});

		t1.start();
		t2.start();
		t3.start();
		t1.join();
	}

}
