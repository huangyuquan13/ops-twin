package com.ops.twin.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.entity.AssetService;
import com.ops.twin.entity.ServiceHostMap;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.AssetHostMapper;
import com.ops.twin.mapper.AssetServiceMapper;
import com.ops.twin.mapper.ServiceHostMapMapper;
import com.ops.twin.mapper.TaskRecordMapper;
import com.ops.twin.websocket.DashboardWebSocketHandler;
import com.ops.twin.websocket.TaskLogWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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

    @Autowired
    private AssetHostMapper assetHostMapper;

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

            boolean isDrill = "DRILL".equals(plan.getPlanType());
            Map<Long, Integer> hostRevertMap = new HashMap<>(); // DRILL 模式记录原始状态

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
                    revertDrillChanges(hostRevertMap, rid);
                    updateStatus(recordId, "CANCELLED", System.currentTimeMillis() - startMs, "用户手动终止");
                    return;
                }

                runStep(rid, step, currentExecIndex++, realStepCount, hostRevertMap, isDrill);
            }

            // DRILL 模式：执行完成后恢复所有主机原状态
            revertDrillChanges(hostRevertMap, rid);

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

    /** 根据 action 映射为目标主机状态：1健康/2报警/3宕机, 返回 null 表示不改变状态 */
    private Integer statusForAction(String action) {
        return switch (action) {
            // 宕机类
            case "STOP_NODE", "SHUTDOWN_IDC"    -> 3;
            // 报警类
            case "INJECT_LOAD", "INJECT_OOM",
                 "FLUSH_CACHE", "EXHAUST_POOL",
                 "ENABLE_MAINTENANCE"           -> 2;
            // 恢复类（只有明确的恢复动作才改状态为健康）
            case "RESTART_NODE", "PROMOTE_SLAVE",
                 "FAILOVER_TO", "LOAD_TEST",
                 "REMOVE_NODE", "SHIFT_TRAFFIC" -> 1;
            // HEALTH_CHECK / VERIFY_* / MONITOR_* 只是观测，不改变主机状态
            default                             -> null;
        };
    }

    /** 执行单个步骤：查找目标主机 → 更新数据库状态 → 广播给终端 + 3D 大屏 */
    private void runStep(String rid, JSONObject step, int current, int total,
                         Map<Long, Integer> hostRevertMap, boolean isDrill)
            throws InterruptedException {
        String action  = step.getString("action");
        String target  = step.getString("target");
        int    waitMs  = step.getIntValue("waitMs", 1500);

        pushLog(rid, String.format("[STEP %d/%d] 正在执行: %s → 目标: %s", current, total, action, target));

        // 查找目标主机
        AssetHost host = null;
        if (target != null && !target.isBlank()) {
            host = assetHostMapper.selectOne(
                new LambdaQueryWrapper<AssetHost>().eq(AssetHost::getHostname, target));
        }

        Integer newStatus = statusForAction(action);

        if (host != null && newStatus != null) {
            Integer oldStatus = host.getStatus();
            // DRILL 模式：记录原始状态，结束后恢复
            if (isDrill && !hostRevertMap.containsKey(host.getId())) {
                hostRevertMap.put(host.getId(), oldStatus);
            }

            // 更新主机状态
            host.setStatus(newStatus);
            assetHostMapper.updateById(host);

            pushLog(rid, String.format("  └─ [联动] %s 状态变更: %d → %d", target, oldStatus, newStatus));

            // 广播 3D 大屏事件
            String event = String.format(
                "{\"type\":\"HOST_STATUS\",\"hostId\":%d,\"hostname\":\"%s\",\"status\":%d,\"action\":\"%s\",\"oldStatus\":%d}",
                host.getId(), host.getHostname(), newStatus, action, oldStatus);
            DashboardWebSocketHandler.broadcast(event);
        } else if (host != null && newStatus == null) {
            // 有目标主机但 action 不改变状态（如 NOTIFY, WAIT）
            pushLog(rid, String.format("  └─ [联动] %s 状态不变, 当前: %d", target, host.getStatus()));
        } else if (target != null && host == null) {
            pushLog(rid, String.format("  └─ [警告] 未找到主机: %s，跳过", target));
        }

        Thread.sleep(waitMs);
        pushLog(rid, String.format("[STEP %d/%d] ✓ 完成", current, total));
    }

    private void pushLog(String recordId, String message) {
        String timestamp = LocalDateTime.now().format(LOG_FMT);
        String line = message.isBlank() ? "" : String.format("[%s] %s", timestamp, message);
        wsHandler.broadcast(recordId, line);
    }

    /** DRILL 模式下恢复所有被修改的主机状态 */
    private void revertDrillChanges(Map<Long, Integer> hostRevertMap, String rid) {
        if (hostRevertMap.isEmpty()) return;
        pushLog(rid, "");
        pushLog(rid, "[INFO] ──────────────────────────────────────────────");
        pushLog(rid, "[INFO] 【DRILL 恢复】正在恢复被演练修改的主机状态...");

        for (Map.Entry<Long, Integer> entry : hostRevertMap.entrySet()) {
            AssetHost host = assetHostMapper.selectById(entry.getKey());
            if (host != null) {
                Integer oldStatus = entry.getValue();
                host.setStatus(oldStatus);
                assetHostMapper.updateById(host);
                pushLog(rid, String.format("  └─ %s 状态恢复: → %d", host.getHostname(), oldStatus));

                String event = String.format(
                    "{\"type\":\"HOST_STATUS\",\"hostId\":%d,\"hostname\":\"%s\",\"status\":%d,\"action\":\"DRILL_REVERT\"}",
                    host.getId(), host.getHostname(), oldStatus);
                DashboardWebSocketHandler.broadcast(event);
            }
        }
        pushLog(rid, "[INFO] DRILL 演练完成，所有主机状态已恢复");
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
