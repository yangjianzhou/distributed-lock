package com.iwill.db.mapper;

import com.iwill.db.model.LockRecordDTO;

public interface LockRecordMapperExt extends LockRecordMapper{

    LockRecordDTO selectByLockName(String lockName);

    int updateExpireTime(LockRecordDTO record);
}