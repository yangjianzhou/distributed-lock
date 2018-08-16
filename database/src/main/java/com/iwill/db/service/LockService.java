package com.iwill.db.service;

import com.iwill.db.mapper.LockRecordMapperExt;
import com.iwill.db.model.LockRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LockService {

    @Autowired
    private LockRecordMapperExt lockRecordMapperExt;

    private ConcurrentMap<Thread, LockData> threadData = new ConcurrentHashMap<>();

    private static class LockData {

        /**
         * 锁的持有线程
         */
        final Thread owningThread;

        /**
         * 锁名称
         */
        final String lockName;

        /**
         * 锁的拥有者
         */
        final String owner;

        /**
         * 当前线程进入锁的次数
         */
        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(Thread owningThread, String lockName, String owner) {
            this.owningThread = owningThread;
            this.lockName = lockName;
            this.owner = owner;
        }
    }

    /**
     * @param lockName 锁名称
     * @param lockTime 锁时间
     * @return
     */
    public boolean acquire(String lockName, Long lockTime) {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData != null) {
            lockData.lockCount.incrementAndGet();
            return true;
        }

        LockRecordDTO lockRecord = lockRecordMapperExt.selectByLockName(lockName);
        if (lockRecord == null) {
            String lockOwner = generatorOwner();
            boolean acquired = tryAcquire(lockName, lockTime, lockOwner);
            if (acquired) {
                startExtendExpireTimeTask(lockName, lockOwner, lockTime);
                lockData = new LockData(currentThread, lockName, lockOwner);
                threadData.put(currentThread, lockData);
            }
            return acquired;
        }

        long lockExpireTime = lockRecord.getExpireTime();
        if (lockExpireTime < System.currentTimeMillis()) {
            String lockOwner = generatorOwner();
            boolean acquired = tryAcquire(lockRecord, lockTime, lockOwner);
            if (acquired) {
                lockData = new LockData(currentThread, lockName, lockOwner);
                threadData.put(currentThread, lockData);
            }
            return acquired;
        }
        return false;
    }

    /**
     * 尝试获得锁，数据库表有设置唯一键约束，只有插入成功的线程才可以获取锁
     * @param lockName 锁名称
     * @param lockTime  锁的过期时间
     * @param lockOwner 锁的拥有者
     * @return
     */
    private boolean tryAcquire(String lockName, long lockTime, String lockOwner) {
        try {
            LockRecordDTO lockRecord = new LockRecordDTO();
            lockRecord.setLockName(lockName);
            Long expireTime = System.currentTimeMillis() + lockTime;
            lockRecord.setExpireTime(expireTime);
            lockRecord.setLockOwner(lockOwner);
            lockRecord.setVersion(0);
            int insertCount = lockRecordMapperExt.insert(lockRecord);
            return insertCount == 1;
        } catch (Exception exp) {
            return false;
        }
    }

    /**
     *  本方法用来延长过期时间，作用就是保持锁不过期，类似与zookeeper分布式锁的会话保持
     *  如果会话一直存在，且锁没有被释放，则锁一直不过期
     * @param lockName 锁名称
     * @param lockOwner 锁的拥有者
     * @param lockTime 锁的过期时间
     */
    private void startExtendExpireTimeTask(String lockName, String lockOwner, long lockTime) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LockRecordDTO lockRecord = lockRecordMapperExt.selectByLockName(lockName);
                if (lockRecord == null || !lockRecord.getLockOwner().equals(lockOwner)) {
                    timer.cancel();
                    return;
                }
                long newExpireTime = System.currentTimeMillis() + lockTime * 3 / 4;
                lockRecord.setExpireTime(newExpireTime);
                lockRecordMapperExt.updateExpireTimeByOwner(lockRecord);
            }
        }, 0, lockTime * 3 / 4);
    }

    /**
     *  当上一次获取锁的线程没有正确释放锁时，下一次其他线程获取锁时会调用本方法
     *  当多个线程竞争获取锁时，有乐观锁控制，只有更新成功的线程才会获的锁
     * @param lockRecord 锁记录，里面保存了上一次获取锁的拥有者信息
     * @param lockTime 锁过期时间
     * @param lockOwner  锁的拥有者
     * @return
     */
    private boolean tryAcquire(LockRecordDTO lockRecord, long lockTime, String lockOwner) {
        try {
            Long expireTime = System.currentTimeMillis() + lockTime;
            lockRecord.setExpireTime(expireTime);
            lockRecord.setLockOwner(lockOwner);
            int updateCount = lockRecordMapperExt.updateExpireTime(lockRecord);
            return updateCount == 1;
        } catch (Exception exp) {
            return false;
        }
    }

    /**
     * 释放锁
     * 实现参考zookeeper的锁释放机制
     */
    public void release() {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData == null) {
            throw new RuntimeException("current thread do not own lock");
        }
        int newLockCount = lockData.lockCount.decrementAndGet();
        if (newLockCount > 0) {
            return;
        }
        if (newLockCount < 0) {
            throw new RuntimeException("Lock count has gone negative for lock :" + lockData.lockName);
        }
        try {
            lockRecordMapperExt.deleteByOwner(lockData.lockName, lockData.owner);
        } finally {
            threadData.remove(currentThread);
        }
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
