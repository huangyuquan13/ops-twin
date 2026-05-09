package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.TaskRecordMapper;
import com.ops.twin.service.TaskExecutionEngine;
import com.ops.twin.service.TaskRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务执行流水 Service 实现类
 */
@Slf4j
@Service
public class TaskRecordServiceImpl extends ServiceImpl<TaskRecordMapper, TaskRecord>
        implements TaskRecordService {

    @Autowired
    private TaskExecutionEngine executionEngine;

    @Override
    public Long triggerAsync(TaskPlan plan, String operator) {
        // 1. 创建初始流水记录（状态：PENDING）
        TaskRecord record = new TaskRecord();
        record.setPlanId(plan.getId());
        record.setPlanName(plan.getPlanName());
        record.setServiceId(plan.getServiceId());
        record.setRunStatus("PENDING");
        record.setOperator(operator);
        record.setResultMsg("等待引擎调度...");
        record.setCreateTime(LocalDateTime.now());
        record.setStartTime(LocalDateTime.now());
        save(record);

        // 2. 调用独立组件的异步方法（此时 @Async 会生效，接口立即返回）
        executionEngine.execute(record.getId(), plan);

        return record.getId();
    }
}
