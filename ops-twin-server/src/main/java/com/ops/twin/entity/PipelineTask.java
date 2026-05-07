package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pipeline_task")
public class PipelineTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long nodeId;
    private String taskType;
    private String status;
    private LocalDateTime createTime;
}
