package com.ops.twin.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.entity.AssetService;
import com.ops.twin.entity.ServiceHostMap;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.AssetServiceMapper;
import com.ops.twin.mapper.ServiceHostMapMapper;
import com.ops.twin.mapper.TaskRecordMapper;
import com.ops.twin.websocket.TaskLogWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

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

    @Autowired
    private AssetServiceMapper assetServiceMapper;

    @Autowired
    private ServiceHostMapMapper serviceHostMapMapper;

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** 取消标志：recordId -> 是否已请求取消 */
    private final ConcurrentHashMap<Long, Boolean> cancelFlags = new ConcurrentHashMap<>();

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

            // 校验关联的逻辑服务是否存在
            if (plan.getServiceId() != null) {
                AssetService svc = assetServiceMapper.selectById(plan.getServiceId());
                if (svc == null) {
                    pushLog(rid, "[ERROR] 关联的逻辑服务（ID=" + plan.getServiceId() + "）不存在，请检查服务映射配置");
                    updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "关联服务不存在");
                    return;
                }
                long hostCount = serviceHostMapMapper.selectCount(
                    new LambdaQueryWrapper<ServiceHostMap>().eq(ServiceHostMap::getServiceId, plan.getServiceId())
                );
                if (hostCount == 0) {
                    pushLog(rid, "[ERROR] 关联服务【" + svc.getServiceName() + "】当前未绑定任何物理主机，无法执行演练");
                    updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "关联服务无可用主机");
                    return;
                } else {
                    pushLog(rid, "[INFO] 关联服务【" + svc.getServiceName() + "】已绑定 " + hostCount + " 台物理主机");
                }
            }

            String stepsJson = plan.getStepsJson();
            if (stepsJson == null || stepsJson.isBlank()) {
                pushLog(rid, "[ERROR] 预案【" + plan.getPlanName() + "】未配置执行步骤，请先在编排页面（workflow）中设计演练流程");
                updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "未配置执行步骤");
                return;
            }

            Object raw = JSON.parse(stepsJson);
            JSONArray steps;
            if (raw instanceof JSONObject) {
                steps = ((JSONObject) raw).getJSONArray("steps");
            } else if (raw instanceof JSONArray) {
                steps = (JSONArray) raw;
            } else {
                log.error("[引擎] 无法识别的 stepsJson 格式");
                return;
            }

            if (steps == null) {
                pushLog(rid, "[ERROR] 预案解析失败：steps 数组为空，请检查编排配置");
                updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "steps 数组为空");
                return;
            }

            int realStepCount = 0;
            for (int i = 0; i < steps.size(); i++) {
                JSONObject step = steps.getJSONObject(i);
                if (step != null && !step.containsKey("isLayoutMeta")) {
                    realStepCount++;
                }
            }

            if (realStepCount == 0) {
                pushLog(rid, "[ERROR] 预案未包含任何可执行步骤，请先在编排页面中设计演练流程");
                updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "无可执行步骤");
                return;
            }

            // 批量校验：需要目标主机的步骤，target 不能为空
            for (int i = 0; i < steps.size(); i++) {
                JSONObject step = steps.getJSONObject(i);
                if (step == null) continue;
                if (step.containsKey("isLayoutMeta") && step.getBooleanValue("isLayoutMeta")) continue;
                String action = step.getString("action");
                String target = step.getString("target");
                if (requiresTarget(action) && (target == null || target.isBlank())) {
                    pushLog(rid, "[ERROR] 步骤【" + action + "】缺少目标对象（Target），请在编排页面中为每个操作节点指定目标主机");
                    updateStatus(recordId, "FAILED", System.currentTimeMillis() - startMs, "步骤缺少目标主机");
                    return;
                }
            }

            int currentExecIndex = 1;
            for (int i = 0; i < steps.size(); i++) {
                JSONObject step = steps.getJSONObject(i);
                if (step == null) continue;

                // 跳过布局元数据
                if (step.containsKey("isLayoutMeta") && step.getBooleanValue("isLayoutMeta")) {
                    continue;
                }

                // 检查是否被取消
                if (Boolean.TRUE.equals(cancelFlags.get(recordId))) {
                    cancelFlags.remove(recordId);
                    pushLog(rid, "");
                    pushLog(rid, "[WARN] ═══════════════════════════════════════════");
                    pushLog(rid, "[WARN]  用户已终止演练任务");
                    pushLog(rid, "[WARN] ═══════════════════════════════════════════");
                    pushLog(rid, "");
                    updateStatus(recordId, "CANCELLED", System.currentTimeMillis() - startMs, "用户手动终止");
                    return;
                }

                runStep(rid, step, currentExecIndex++, realStepCount);
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

    /**
     * 请求取消指定任务
     */
    public void cancel(Long recordId) {
        cancelFlags.put(recordId, true);
    }

    /** 判断动作类型是否需要指定目标主机 */
    private boolean requiresTarget(String action) {
        if (action == null) return true;
        return switch (action) {
            case "WAIT", "NOTIFY" -> false;
            default -> true;
        };
    }

    private void runStep(String rid, JSONObject step, int current, int total) throws InterruptedException {
        String action  = step.getString("action");
        String target  = step.getString("target");
        int    waitMs  = step.getIntValue("waitMs", 1500);

        pushLog(rid, String.format("[STEP %d/%d] 正在执行: %s → 目标: %s", current, total, action, target));
        Thread.sleep(waitMs);
        pushLog(rid, String.format("[STEP %d/%d] ✓ 完成", current, total));
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
        if ("SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
            record.setEndTime(LocalDateTime.now());
        }
        // 使用 Mapper 直接更新，不依赖 Service 接口
        taskRecordMapper.updateById(record);
    }
}
