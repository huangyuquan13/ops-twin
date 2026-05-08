package com.ops.twin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ops.twin.entity.TaskPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演练预案 Mapper 接口（MyBatis-Plus 自动提供基础 CRUD）
 */
@Mapper
public interface TaskPlanMapper extends BaseMapper<TaskPlan> {
}
