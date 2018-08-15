package com.iwill.db.service;

import com.iwill.db.mapper.LockRecordMapperExt;
import com.iwill.db.model.LockRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LockService {

    @Autowired
    private LockRecordMapperExt lockRecordMapperExt;

    private ConcurrentMap<Thread, LockData> threadData = new ConcurrentHashMap<>();

    private static class LockData {
        final Thread owningThread;
        final String lockPath;
        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(Thread owningThread, String lockPath) {
            this.owningThread = owningThread;
            this.lockPath = lockPath;
        }
    }

    /**
     * TODO :还需要检查锁是否过期
     * @param lockName
     * @param lockTime
     * @return
     */
    public boolean acquireLock(String lockName, Long lockTime) {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData != null) {
            lockData.lockCount.incrementAndGet();
            return true;
        }

        LockRecordDTO lockRecord = lockRecordMapperExt.selectByLockName(lockName);
        if (lockRecord == null) {
            boolean acquired = tryAcquireLock(lockName, lockTime);
            if (acquired) {
                lockData = new LockData(currentThread, lockName);
                threadData.put(currentThread, lockData);
            }
            return acquired;
        }

        long lockExpireTime = lockRecord.getExpireTime();
        if (lockExpireTime < System.currentTimeMillis()) {
            boolean acquired = tryAcquireLock(lockRecord, lockTime);
            if (acquired) {
                lockData = new LockData(currentThread, lockName);
                threadData.put(currentThread, lockData);
            }
            return acquired;
        }
        return false;
    }

    private boolean tryAcquireLock(String lockName, Long lockTime) {
        try {
            LockRecordDTO lockRecord = new LockRecordDTO();
            lockRecord.setLockName(lockName);
            Long expireTime = System.currentTimeMillis() + lockTime;
            lockRecord.setExpireTime(expireTime);
            lockRecord.setVersion(0);
            int insertCount = lockRecordMapperExt.insert(lockRecord);
            return insertCount == 1;
        } catch (Exception exp) {
            return false;
        }
    }

    private boolean tryAcquireLock(LockRecordDTO lockRecord, long lockTime) {
        try {
            Long expireTime = System.currentTimeMillis() + lockTime;
            lockRecord.setExpireTime(expireTime);
            int updateCount = lockRecordMapperExt.updateExpireTime(lockRecord);
            return updateCount == 1;
        } catch (Exception exp) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception{
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("this is a timer");
                timer.cancel();
            }
        },0,100);

        Thread.sleep(1000L);
    }
}
