package com.ops.twin.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.audit.AuditLog;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.entity.AssetService;
import com.ops.twin.entity.AuditEvent;
import com.ops.twin.entity.ServiceHostMap;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.mapper.AssetHostMapper;
import com.ops.twin.mapper.AssetServiceMapper;
import com.ops.twin.mapper.ServiceHostMapMapper;
import com.ops.twin.service.AuditEventService;
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
public class TaskPlanController {

    @Autowired
    private TaskPlanService taskPlanService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AssetHostMapper assetHostMapper;

    @Autowired
    private ServiceHostMapMapper serviceHostMapMapper;

    @Autowired
    private AssetServiceMapper assetServiceMapper;

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
    @AuditLog(operation = "CREATE_PLAN", description = "新增预案")
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

        boolean isNew = plan.getId() == null;
        taskPlanService.saveOrUpdate(plan);
        AuditEvent audit = new AuditEvent();
        audit.setOperator("admin");
        audit.setEventType(isNew ? "CREATE_PLAN" : "UPDATE_PLAN");
        audit.setDetail((isNew ? "新增预案【" : "编辑预案【") + plan.getPlanName() + "】");
        auditEventService.save(audit);
        return Result.success(plan);
    }

    /**
     * 删除预案
     */
    @AuditLog(operation = "DELETE_PLAN", description = "删除预案")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        TaskPlan plan = taskPlanService.getById(id);
        if (plan != null) {
            AuditEvent audit = new AuditEvent();
            audit.setOperator("admin");
            audit.setEventType("DELETE_PLAN");
            audit.setDetail("删除预案【" + plan.getPlanName() + "】");
            auditEventService.save(audit);
        }
        return Result.success(taskPlanService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<TaskPlan> getById(@PathVariable Long id) {
        return Result.success(taskPlanService.getById(id));
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

    /**
     * 重置预案执行环境：恢复主机状态 + 重建服务绑定 + 重新启用预案
     * POST /api/task/plan/reset/{planId}
     */
    @PostMapping("/reset/{planId}")
    public Result<String> reset(@PathVariable Long planId) {
        TaskPlan plan = taskPlanService.getById(planId);
        if (plan == null) return Result.error("预案不存在");
        if (!"FAILOVER".equals(plan.getPlanType()) && !"SCALE".equals(plan.getPlanType())) {
            return Result.error("仅故障切换和扩缩容预案需要重置");
        }

        Long svcId = plan.getServiceId();
        int restoredHosts = 0;
        if (svcId != null) {
            AssetService svc = assetServiceMapper.selectById(svcId);
            if (svc != null && svc.getTopologyJson() != null) {
                Object raw = JSON.parse(svc.getTopologyJson());
                JSONArray arr = raw instanceof JSONArray ? (JSONArray) raw : null;
                if (arr != null && !arr.isEmpty()) {
                    // 删旧绑定
                    LambdaQueryWrapper<ServiceHostMap> delW = new LambdaQueryWrapper<>();
                    delW.eq(ServiceHostMap::getServiceId, svcId);
                    serviceHostMapMapper.delete(delW);

                    // 从 topology 重建绑定 + 恢复主机
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject node = arr.getJSONObject(i);
                        if (node == null) continue;
                        if ("edge".equals(node.getString("type"))) continue;
                        JSONObject data = node.getJSONObject("data");
                        if (data == null) continue;
                        Long hostId = data.getLong("hostId");
                        if (hostId == null) continue;

                        ServiceHostMap map = new ServiceHostMap();
                        map.setServiceId(svcId);
                        map.setHostId(hostId);
                        serviceHostMapMapper.insert(map);

                        AssetHost host = assetHostMapper.selectById(hostId);
                        if (host != null && host.getStatus() != 1) {
                            host.setStatus(1);
                            assetHostMapper.updateById(host);
                            restoredHosts++;
                        }
                    }
                }
            }
        }
        plan.setStatus(1);
        taskPlanService.updateById(plan);

        return Result.success("已重置，恢复 " + restoredHosts + " 台主机，重建服务绑定");
    }
}
