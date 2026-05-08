package com.ops.twin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ops.twin.entity.TaskRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行流水 Mapper 接口
 */
@Mapper
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {
}
