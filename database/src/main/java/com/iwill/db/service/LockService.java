package com.iwill.db.service;

import com.iwill.db.mapper.LockRecordMapperExt;
import com.iwill.db.model.LockRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LockService {

    @Autowired
    private LockRecordMapperExt lockRecordMapperExt;

    public boolean acquireLock(String lockName, Long lockTime) {
        LockRecordDTO lockRecord = lockRecordMapperExt.selectByLockName(lockName);
        if (lockRecord == null) {
            return tryAcquireLock(lockName, lockTime);
        }

        long lockExpireTime = lockRecord.getExpireTime();
        if (lockExpireTime < System.currentTimeMillis()) {
            return tryAcquireLock(lockRecord, lockTime);
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
}
