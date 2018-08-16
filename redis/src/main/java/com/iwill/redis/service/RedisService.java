package com.iwill.redis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RedisService {

    @Autowired
    private Jedis client;

    private ConcurrentMap<Thread, LockData> threadData = new ConcurrentHashMap<>();

    private static String lockScriptSHA;

    private static String unlockScriptSHA;

    private static String lockScript = "";

    private static String unlockScript = "";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static class LockData {

        /**
         * 锁的持有线程
         */
        final Thread owningThread;

        /**
         * key
         */
        final String key;

        /**
         * 锁的拥有者
         */
        final String owner;

        /**
         * 当前线程进入锁的次数
         */
        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(Thread owningThread, String key, String owner) {
            this.owningThread = owningThread;
            this.key = key;
            this.owner = owner;
        }
    }

    public boolean acquire(String key, long expireTime) {

        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData != null) {
            lockData.lockCount.incrementAndGet();
            return true;
        }

        if (lockScriptSHA == null) {
            lockScriptSHA = client.scriptLoad(lockScript);
        }
        String owner = generatorOwner();
        boolean acquired = false;
        try {
            acquired = (Boolean) client.evalsha(lockScriptSHA, 1, key, owner, String.valueOf(expireTime));
        } catch (Exception exp) {
            logger.error("execute eval sha throw exp", exp);
        }
        if (acquired) {
            lockData = new LockData(currentThread, key, owner);
            threadData.put(currentThread, lockData);
            return true;
        }

        return false;
    }

    public boolean release() {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData == null) {
            throw new RuntimeException("current thread do not own lock");
        }
        int newLockCount = lockData.lockCount.decrementAndGet();
        if (newLockCount > 0) {
            return true;
        }
        if (newLockCount < 0) {
            throw new RuntimeException("Lock count has gone negative for lock :" + lockData.key);
        }
        if (unlockScriptSHA == null) {
            unlockScriptSHA = client.scriptLoad(unlockScript);
        }
        try {
            client.evalsha(unlockScriptSHA, 1, lockData.key, lockData.owner);
        } finally {
            threadData.remove(currentThread);
        }
        return true;
    }

    /**
     * 作为当前线程的唯一标识
     *
     * @return
     */
    private String generatorOwner() {
        return UUID.randomUUID().toString();
    }
}
