package fi.luomus.triplestore.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class AccessLimiter {

	public static class TooManyRequestsException extends Exception {
		private static final long serialVersionUID = -3769561443667075468L;
		public TooManyRequestsException(int maxPerUser, String userId) {
			super("Too many pending requests from user '" + userId + "'. Max allowed: " + maxPerUser);
		}
	}

	public interface Access {
		void release();
		static Access noLimit() {
			return () -> {};
		}
	}

	private static class AccessImpl implements Access {
		private final AccessLimiter granter;
		private final String userId;
		private boolean released = false;

		AccessImpl(AccessLimiter granter, String userId) {
			this.granter = granter;
			this.userId = userId;
		}

		@Override
		public synchronized void release() {
			if (!released) {
				granter.release(userId);
				released = true;
			}
		}
	}

	private final Map<String, Semaphore> userSemaphores = new ConcurrentHashMap<>();
	private final int maxPerUser;

	/**
	 * @param maxPerUser maximum simultaneous accesses per user
	 */
	public AccessLimiter(int maxPerUser) {
		if (maxPerUser <= 0) throw new IllegalArgumentException("maxPerUser must be > 0");
		this.maxPerUser = maxPerUser;
	}

	public Access acquire(String userId) throws TooManyRequestsException {
		if (userId == null) throw new IllegalArgumentException("no api user");

		Semaphore sem = userSemaphores.computeIfAbsent(userId, k -> new Semaphore(maxPerUser));

		if (!sem.tryAcquire()) {
			throw new TooManyRequestsException(maxPerUser, userId);
		}

		return new AccessImpl(this, userId);
	}

	private void release(String userId) {
		Semaphore sem = userSemaphores.get(userId);
		if (sem != null) {
			sem.release();
			if (sem.availablePermits() == maxPerUser) {
				userSemaphores.remove(userId, sem);
			}
		}
	}

}
