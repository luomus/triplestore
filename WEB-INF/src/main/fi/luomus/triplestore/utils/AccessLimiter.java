package fi.luomus.triplestore.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.luomus.commons.utils.Utils;

public class AccessLimiter {

	public class AccessNotGrantedTooManyPendingRequests extends Exception {
		private static final long serialVersionUID = -3769561443667075468L;
		public AccessNotGrantedTooManyPendingRequests(int pendingRequests, int maxPerUser, String remoteUser) {
			super("There are too many pending requests from user '" + remoteUser + "' " + pendingRequests+"/"+maxPerUser);
		}
	}

	public static class Access {
		private final AccessLimiter granter;
		private final String id;
		private final String remoteUser;
		private Access(AccessLimiter granter, String remoteUser) {
			this.id = Utils.generateGUID();
			this.granter = granter;
			this.remoteUser = remoteUser;
		}
		public void release() {
			granter.release(this);
		}
		@Override
		public int hashCode() {
			return id.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Access other = (Access) obj;
			return id.equals(other.id);
		}
	}

	private final Map<String, Set<Access>> open = new ConcurrentHashMap<>();
	private final int maxPerUser;

	public AccessLimiter(int maxPerUser) {
		this.maxPerUser = maxPerUser;
	}

	public Access delayAccessIfNecessary(String remoteUser) throws AccessNotGrantedTooManyPendingRequests {
		if (remoteUser == null) remoteUser = "NULL";
		Access access = new Access(this, remoteUser);
		AtomicBoolean tooManyPending = new AtomicBoolean(false);
		open.compute(remoteUser, (key, grantedAccesses) -> {
			if (grantedAccesses == null) {
				grantedAccesses = Collections.newSetFromMap(new ConcurrentHashMap<>());
				grantedAccesses.add(access);
				return grantedAccesses;
			}

			int i = 0;
			while (grantedAccesses.size() >= maxPerUser) {
				if (i++ > 3) {
					tooManyPending.set(true);
					return grantedAccesses;
				}
				sleep();
			}

			grantedAccesses.add(access);
			return grantedAccesses;
		});

		if (tooManyPending.get()) {
			throw new AccessNotGrantedTooManyPendingRequests(open.size(), maxPerUser, remoteUser);
		}

		return access;
	}

	private void sleep() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
	}

	private void release(Access access) {
		getAccesses(access.remoteUser).remove(access);
	}

	private Set<Access> getAccesses(String remoteUser) {
		open.putIfAbsent(remoteUser, Collections.newSetFromMap(new ConcurrentHashMap<Access, Boolean>()));
		return open.get(remoteUser);
	}

}
