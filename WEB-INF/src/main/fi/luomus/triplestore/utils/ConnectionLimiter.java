package fi.luomus.triplestore.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fi.luomus.commons.utils.Utils;

public class ConnectionLimiter {

	public class AccessNotGrantedTooManyPendingRequests extends Exception {
		private static final long serialVersionUID = -3769561443667075468L;
		public AccessNotGrantedTooManyPendingRequests(int pendingRequests, int maxPerUser, String remoteUser) {
			super("There are too many pending requests from user '" + remoteUser + "' " + pendingRequests+"/"+maxPerUser);
		}
	}

	public static class Access {
		private final ConnectionLimiter granter;
		private final String id;
		private final String remoteUser;
		private Access(ConnectionLimiter granter, String remoteUser) {
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

	public ConnectionLimiter(int maxPerUser) {
		this.maxPerUser = maxPerUser;
	}

	public Access delayAccessIfNecessary(String remoteUser) throws AccessNotGrantedTooManyPendingRequests {
		if (remoteUser == null) remoteUser = "NULL";
		Set<Access> opened = getAccesses(remoteUser);
		int i = 0;
		while (opened.size() >= maxPerUser) {
			if (i++ > 3) {
				throw new AccessNotGrantedTooManyPendingRequests(opened.size(), maxPerUser, remoteUser);
			}
			sleep();
		}
		Access access = new Access(this, remoteUser); 
		opened.add(access);
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
		Set<Access> set = open.get(remoteUser);
		if (set == null) {
			set = Collections.newSetFromMap(new ConcurrentHashMap<Access, Boolean>());
			open.put(remoteUser, set);
		}
		return set;
	}

}
