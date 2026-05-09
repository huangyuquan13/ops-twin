package com.ops.twin.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.TaskRecordMapper;
import com.ops.twin.websocket.TaskLogWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 演练执行引擎组件
 * 独立出来是为了确保 @Async 注解生效，实现真正的异步调度
 */
@Slf4j
@Component
public class TaskExecutionEngine {

    @Autowired
    private TaskLogWebSocketHandler wsHandler;

    /** 直接依赖 Mapper 而非 Service，打破循环依赖 */
    @Autowired
    private TaskRecordMapper taskRecordMapper;

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 核心异步执行方法
     */
    @Async
    public void execute(Long recordId, TaskPlan plan) {
        long startMs = System.currentTimeMillis();
        String rid = String.valueOf(recordId);

        try {
            updateStatus(recordId, "RUNNING", null, "引擎已启动，正在执行演练步骤...");

            pushLog(rid, "");
            pushLog(rid, "╔══════════════════════════════════════════════════════╗");
            pushLog(rid, "║          【智维方舟 · 演练引擎】  任务启动             ║");
            pushLog(rid, "╚══════════════════════════════════════════════════════╝");
            pushLog(rid, "");
            pushLog(rid, String.format("[INFO] 预案名称: %s", plan.getPlanName()));
            pushLog(rid, String.format("[INFO] 预案类型: %s  |  优先级: %d", plan.getPlanType(), plan.getPriority()));
            pushLog(rid, String.format("[INFO] 任务流水 ID: %d", recordId));
            pushLog(rid, "[INFO] ──────────────────────────────────────────────");
            Thread.sleep(600);

            String stepsJson = plan.getStepsJson();
            if (stepsJson == null || stepsJson.isBlank()) {
                runDefaultDemo(rid);
            } else {
                JSONArray steps = JSON.parseArray(stepsJson);
                for (int i = 0; i < steps.size(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    runStep(rid, step, i + 1, steps.size());
                }
            }

            long durationMs = System.currentTimeMillis() - startMs;
            double durationSec = durationMs / 1000.0;

            pushLog(rid, "");
            pushLog(rid, "[INFO] ──────────────────────────────────────────────");
            pushLog(rid, String.format("[SUCCESS] 演练任务执行成功，耗时 %.1fs", durationSec));
            pushLog(rid, "╔══════════════════════════════════════════════════════╗");
            pushLog(rid, "║              ✓  ALL STEPS COMPLETED                  ║");
            pushLog(rid, "╚══════════════════════════════════════════════════════╝");
            pushLog(rid, "");

            updateStatus(recordId, "SUCCESS", durationMs, String.format("演练成功，耗时 %.1f 秒", durationSec));

        } catch (Exception e) {
            log.error("[引擎] 演练执行异常：recordId={}", recordId, e);
            pushLog(rid, "[ERROR] 演练引擎异常：" + e.getMessage());
            updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "引擎异常：" + e.getMessage());
        }
    }

    private void runStep(String rid, JSONObject step, int current, int total) throws InterruptedException {
        String action  = step.getString("action");
        String target  = step.getString("target");
        int    waitMs  = step.getIntValue("waitMs", 1500);

        pushLog(rid, String.format("[STEP %d/%d] 正在执行: %s → 目标: %s", current, total, action, target));
        Thread.sleep(waitMs);
        pushLog(rid, String.format("[STEP %d/%d] ✓ 完成", current, total));
    }

    private void runDefaultDemo(String rid) throws InterruptedException {
        String[][] demo = {
            { "HEALTH_CHECK",  "全量节点", "1000" },
            { "STOP_NODE",     "Pay-DB-Master", "2000" },
            { "PROMOTE_SLAVE", "Pay-DB-Slave", "1500" },
            { "HEALTH_CHECK",  "新主节点", "1000" }
        };
        for (int i = 0; i < demo.length; i++) {
            pushLog(rid, String.format("[STEP %d/%d] 正在执行: %s → 目标: %s", i + 1, demo.length, demo[i][0], demo[i][1]));
            Thread.sleep(Integer.parseInt(demo[i][2]));
            pushLog(rid, String.format("[STEP %d/%d] ✓ 完成", i + 1, demo.length));
        }
    }

    private void pushLog(String recordId, String message) {
        String timestamp = LocalDateTime.now().format(LOG_FMT);
        String line = message.isBlank() ? "" : String.format("[%s] %s", timestamp, message);
        wsHandler.broadcast(recordId, line);
    }

    private void updateStatus(Long recordId, String status, Long durationMs, String resultMsg) {
        TaskRecord record = new TaskRecord();
        record.setId(recordId);
        record.setRunStatus(status);
        record.setDurationMs(durationMs);
        record.setResultMsg(resultMsg);
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            record.setEndTime(LocalDateTime.now());
        }
        // 使用 Mapper 直接更新，不依赖 Service 接口
        taskRecordMapper.updateById(record);
    }
}
