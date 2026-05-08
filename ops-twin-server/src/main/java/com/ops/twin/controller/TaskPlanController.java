package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.common.Result;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.service.TaskPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 演练预案管理控制器
 * 接口前缀：/api/task/plan
 */
@RestController
@RequestMapping("/api/task/plan")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskPlanController {

    @Autowired
    private TaskPlanService taskPlanService;

    /**
     * 分页查询预案列表
     * 支持按预案名称和绑定的逻辑服务 ID 筛选
     */
    @GetMapping("/list")
    public Result<Page<TaskPlan>> list(
            @RequestParam(defaultValue = "1")  Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false)    String  planName,
            @RequestParam(required = false)    Long    serviceId,
            @RequestParam(required = false)    String  planType) {

        Page<TaskPlan> page = new Page<>(current, size);
        LambdaQueryWrapper<TaskPlan> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(planName)) {
            wrapper.like(TaskPlan::getPlanName, planName);
        }
        if (serviceId != null) {
            wrapper.eq(TaskPlan::getServiceId, serviceId);
        }
        if (StringUtils.hasText(planType)) {
            wrapper.eq(TaskPlan::getPlanType, planType);
        }
        wrapper.orderByAsc(TaskPlan::getPriority).orderByDesc(TaskPlan::getCreateTime);

        return Result.success(taskPlanService.page(page, wrapper));
    }

    /**
     * 新增或更新预案
     */
    @PostMapping("/save")
    public Result<TaskPlan> save(@RequestBody TaskPlan plan) {
        // 校验同一逻辑服务下预案名称唯一
        LambdaQueryWrapper<TaskPlan> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(TaskPlan::getPlanName, plan.getPlanName())
                   .eq(TaskPlan::getServiceId, plan.getServiceId());
        if (plan.getId() != null) {
            nameWrapper.ne(TaskPlan::getId, plan.getId());
        }
        if (taskPlanService.count(nameWrapper) > 0) {
            return Result.error("该服务下已存在同名预案：" + plan.getPlanName());
        }

        taskPlanService.saveOrUpdate(plan);
        return Result.success(plan);
    }

    /**
     * 删除预案
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return taskPlanService.removeById(id)
                ? Result.success(true)
                : Result.error("删除失败，预案不存在");
    }

    /**
     * 启用 / 禁用预案
     */
    @PutMapping("/toggle/{id}")
    public Result<Boolean> toggle(@PathVariable Long id) {
        TaskPlan plan = taskPlanService.getById(id);
        if (plan == null) return Result.error("预案不存在");
        plan.setStatus(plan.getStatus() == 1 ? 0 : 1);
        return Result.success(taskPlanService.updateById(plan));
    }
}
