package be.waines.maven.incremental.distributed.server.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class LockManager<T> {

	private ConcurrentMap<T, ReadWriteLock> lockMap = new ConcurrentHashMap<T, ReadWriteLock>();
	
	public ReadWriteLock getLock(T key) {
		ReadWriteLock currentLock = lockMap.get(key);
		if (currentLock != null) {
			return currentLock;
		}
		ReadWriteLock newLock = new ReentrantReadWriteLock();
		currentLock = lockMap.putIfAbsent(key, newLock);
		if (currentLock == null) {
			return newLock;
		} else {
			return currentLock;
		}
	}
	
	public void removeLock(T key) {
		Lock writeLock = getLock(key).writeLock();
		try {
			writeLock.tryLock(5, TimeUnit.SECONDS);
			lockMap.remove(key);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			writeLock.unlock();
		}
		
	}
	
}
