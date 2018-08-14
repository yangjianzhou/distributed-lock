package com.iwill.db.mapper;

import com.iwill.db.model.LockRecordDTO;
import com.iwill.db.model.LockRecordDTOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LockRecordMapper {
    long countByExample(LockRecordDTOExample example);

    int deleteByExample(LockRecordDTOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(LockRecordDTO record);

    int insertSelective(LockRecordDTO record);

    List<LockRecordDTO> selectByExample(LockRecordDTOExample example);

    LockRecordDTO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") LockRecordDTO record, @Param("example") LockRecordDTOExample example);

    int updateByExample(@Param("record") LockRecordDTO record, @Param("example") LockRecordDTOExample example);

    int updateByPrimaryKeySelective(LockRecordDTO record);

    int updateByPrimaryKey(LockRecordDTO record);
}