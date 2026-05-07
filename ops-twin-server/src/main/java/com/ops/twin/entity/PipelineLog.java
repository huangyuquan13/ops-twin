package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pipeline_log")
public class PipelineLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String logContent;
    private LocalDateTime createTime;
}
