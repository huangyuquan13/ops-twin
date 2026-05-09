package com.ops.twin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;

/**
 * 任务执行流水 Service 接口
 * 扩展了演练引擎的异步触发能力
 */
public interface TaskRecordService extends IService<TaskRecord> {

    /**
     * 异步触发演练预案执行
     * 立即返回 recordId，后台异步执行步骤并通过 WebSocket 推日志
     *
     * @param plan     演练预案
     * @param operator 执行人
     * @return 新建的任务流水记录 ID
     */
    Long triggerAsync(TaskPlan plan, String operator);
}
