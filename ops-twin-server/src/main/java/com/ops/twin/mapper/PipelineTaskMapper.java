package com.ops.twin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ops.twin.entity.PipelineTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineTaskMapper extends BaseMapper<PipelineTask> {
}
