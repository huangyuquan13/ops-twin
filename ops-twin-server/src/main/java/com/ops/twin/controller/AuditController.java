package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AuditEvent;
import com.ops.twin.service.AuditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 操作审计控制器
 * 接口前缀：/api/audit
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuditController {

    @Autowired
    private AuditEventService auditEventService;

    /** 分页查询审计日志 */
    @GetMapping("/list")
    public Result<Page<AuditEvent>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String eventType) {

        Page<AuditEvent> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditEvent> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            wrapper.like(AuditEvent::getOperator, operator);
        }
        if (StringUtils.hasText(eventType)) {
            wrapper.eq(AuditEvent::getEventType, eventType);
        }
        wrapper.orderByDesc(AuditEvent::getCreateTime);
        return Result.success(auditEventService.page(page, wrapper));
    }

    /** 手动写入一条审计日志（内部调用） */
    @PostMapping("/log")
    public Result<Boolean> log(@RequestBody Map<String, String> body) {
        AuditEvent event = new AuditEvent();
        event.setOperator(body.getOrDefault("operator", "system"));
        event.setEventType(body.getOrDefault("eventType", "OTHER"));
        event.setDetail(body.getOrDefault("detail", ""));
        return auditEventService.save(event) ? Result.success(true) : Result.error("记录失败");
    }

    /** 查询所有事件类型 */
    @GetMapping("/types")
    public Result<java.util.List<String>> types() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        auditEventService.list().forEach(e -> {
            if (e.getEventType() != null) set.add(e.getEventType());
        });
        return Result.success(new java.util.ArrayList<>(set));
    }
}
