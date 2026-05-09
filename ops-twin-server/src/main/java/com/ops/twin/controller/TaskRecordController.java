package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.common.Result;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.service.TaskPlanService;
import com.ops.twin.service.TaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 任务执行流水控制器
 * 接口前缀：/api/task/record
 */
@RestController
@RequestMapping("/api/task/record")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskRecordController {

    @Autowired
    private TaskRecordService taskRecordService;

    @Autowired
    private TaskPlanService taskPlanService;

    /**
     * 触发演练预案执行
     * POST /api/task/record/trigger/{planId}
     * 立即返回 recordId，后台异步执行演练引擎并通过 WebSocket 推流日志
     *
     * @param planId   演练预案 ID
     * @param operator 执行人（可选，默认 admin）
     * @return recordId（前端用它订阅 WebSocket 频道）
     */
    @PostMapping("/trigger/{planId}")
    public Result<Map<String, Object>> trigger(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "admin") String operator) {

        // 1. 校验预案是否存在且已启用
        TaskPlan plan = taskPlanService.getById(planId);
        if (plan == null) {
            return Result.error("预案不存在，ID：" + planId);
        }
        if (plan.getStatus() == null || plan.getStatus() != 1) {
            return Result.error("预案【" + plan.getPlanName() + "】当前处于禁用状态，无法执行");
        }

        // 2. 触发异步引擎，获取 recordId
        Long recordId = taskRecordService.triggerAsync(plan, operator);

        // 3. 返回 recordId 及 WebSocket 订阅路径（供前端直接拼接连接）
        return Result.success(Map.of(
            "recordId",   recordId,
            "wsPath",     "/ws/task/log/" + recordId,
            "planName",   plan.getPlanName()
        ));
    }

    /**
     * 分页查询任务执行流水列表
     * GET /api/task/record/list
     */
    @GetMapping("/list")
    public Result<Page<TaskRecord>> list(
            @RequestParam(defaultValue = "1")  Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false)    Long    planId) {

        Page<TaskRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<TaskRecord> wrapper = new LambdaQueryWrapper<>();
        if (planId != null) {
            wrapper.eq(TaskRecord::getPlanId, planId);
        }
        wrapper.orderByDesc(TaskRecord::getCreateTime);
        return Result.success(taskRecordService.page(page, wrapper));
    }

    /**
     * 查询单条任务流水详情（用于轮询最终状态）
     * GET /api/task/record/{id}
     */
    @GetMapping("/{id}")
    public Result<TaskRecord> getById(@PathVariable Long id) {
        TaskRecord record = taskRecordService.getById(id);
        return record != null ? Result.success(record) : Result.error("流水记录不存在");
    }

    /**
     * 终止正在执行的演练任务
     * POST /api/task/record/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    public Result<String> terminate(@PathVariable Long id) {
        boolean ok = taskRecordService.terminate(id);
        if (ok) {
            return Result.success("已发送终止信号");
        }
        return Result.error("任务不存在或已结束，无法终止");
    }
}
