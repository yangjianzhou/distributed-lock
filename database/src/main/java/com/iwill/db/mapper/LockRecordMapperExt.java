package com.iwill.db.mapper;

import com.iwill.db.model.LockRecordDTO;
import org.apache.ibatis.annotations.Param;

public interface LockRecordMapperExt extends LockRecordMapper{

    LockRecordDTO selectByLockName(String lockName);

    int updateExpireTime(LockRecordDTO record);

    int updateExpireTimeByOwner(LockRecordDTO record);

    int deleteByOwner(@Param("lockName") String lockName , @Param("lockOwner") String lockOwner) ;
}