package com.ops.twin.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.TaskRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时清理上次异常退出残留的 RUNNING/PENDING 任务记录。
 * 服务器重启意味着所有异步执行线程已死，这些记录不可能再完成。
 */
@Slf4j
@Component
public class StaleTaskCleanup {

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void cancelStaleTasks() {
        TaskRecord update = new TaskRecord();
        update.setRunStatus("CANCELLED");
        update.setEndTime(LocalDateTime.now());
        update.setResultMsg("系统重启，任务自动取消");

        LambdaUpdateWrapper<TaskRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(TaskRecord::getRunStatus, "PENDING", "RUNNING");

        int count = taskRecordMapper.update(update, wrapper);
        if (count > 0) {
            log.info("[启动清理] 已将 {} 条残留的 RUNNING/PENDING 任务记录标记为 CANCELLED", count);
        }
    }
}
