package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 演练预案实体类（对应 task_plan 表）
 */
@Data
@TableName("task_plan")
public class TaskPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预案名称 */
    private String planName;

    /** 绑定的逻辑服务 ID，关联 asset_service 表 */
    private Long serviceId;

    /** 预案类型：DRILL(演练) / FAILOVER(故障切换) / SCALE(扩缩容) */
    private String planType;

    /** 优先级：1(高) / 2(中) / 3(低) */
    private Integer priority;

    /** 执行步骤的 JSON 编排 */
    private String stepsJson;

    /** 预案说明 */
    private String description;

    /** 状态：1(启用) / 0(禁用) */
    private Integer status;

    /** 创建人 */
    private String createBy;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
