package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务执行流水实体类（对应 task_record 表）
 */
@Data
@TableName("task_record")
public class TaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的预案 ID */
    private Long planId;

    /** 冗余预案名称（防止预案删除后查询失效） */
    private String planName;

    /** 关联的逻辑服务 ID */
    private Long serviceId;

    /** 执行状态：PENDING / RUNNING / SUCCESS / FAILED */
    private String runStatus;

    /** 总耗时（毫秒） */
    private Long durationMs;

    /** 操作人 */
    private String operator;

    /** 执行结果简要描述 */
    private String resultMsg;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
