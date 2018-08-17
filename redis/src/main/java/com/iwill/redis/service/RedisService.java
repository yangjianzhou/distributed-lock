package com.iwill.redis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Timer;
import java.util.TimerTask;
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

    private static String extendExpireTimeScriptSHA;

    private static String lockScript                = "local key = KEYS[1]                            \n"
                                                    + "local value = ARGV[1]                          \n"
                                                    + "local expireTime = ARGV[2]                     \n"
                                                    + "                                               \n"
                                                    + "if (redis.call('setnx',key,value) == 1) then   \n"
                                                    + "   redis.call('pexpire' , key , expireTime)    \n"
                                                    + "   return 'true'                               \n"
                                                    + "else                                           \n"
                                                    + "   return 'false'                              \n"
                                                    + "end                                            \n";

    private static String unlockScript              = "local key = KEYS[1]                            \n"
                                                    + "local value = ARGV[1]                          \n"
                                                    + "                                               \n"
                                                    + "if (redis.call('get',key) == value)  then      \n"
                                                    + "   redis.call('del' , key )                    \n"
                                                    + "   return 'true'                               \n"
                                                    + "else                                           \n"
                                                    + "   return 'false'                              \n"
                                                    + "end                                            \n";

    private static String extendExpireTimeScript    = "local key = KEYS[1]                            \n"
                                                    + "local value = ARGV[1]                          \n"
                                                    + "local newExpireTime = ARGV[2]                  \n"
                                                    + "                                               \n"
                                                    + "if (redis.call('get',key) == value)  then      \n"
                                                    + "   redis.call('pexpire' , key ,newExpireTime)  \n"
                                                    + "   return 'true'                               \n"
                                                    + "else                                           \n"
                                                    + "   return 'false'                              \n"
                                                    + "end                                            \n";
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
            Object result = client.evalsha(lockScriptSHA, 1, key, owner, String.valueOf(expireTime));
            acquired = Boolean.valueOf((String) result);
        } catch (Exception exp) {
            logger.error("execute eval sha throw exp", exp);
        }
        if (acquired) {
            startExtendExpireTimeTask(key, owner, expireTime);
            lockData = new LockData(currentThread, key, owner);
            threadData.put(currentThread, lockData);
            return true;
        }

        return false;
    }

    private void startExtendExpireTimeTask(String key, String owner, long expireTime) {
        if (extendExpireTimeScriptSHA == null) {
            extendExpireTimeScriptSHA = client.scriptLoad(extendExpireTimeScript);
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Object result = client.evalsha(extendExpireTimeScriptSHA, 1, key, owner, String.valueOf(expireTime));
                    boolean extendSuccess = Boolean.valueOf((String) result);
                    if (!extendSuccess) {
                        timer.cancel();
                    }
                } catch (Exception exp) {
                    timer.cancel();
                }
            }
        }, 0, expireTime * 3 / 4);
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
            Object result = client.evalsha(unlockScriptSHA, 1, lockData.key, lockData.owner);
            boolean unlocked = Boolean.valueOf((String) result);
            if (!unlocked) {
                logger.error(String.format("unlock fail ,key = %s", lockData.key));
            }
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
