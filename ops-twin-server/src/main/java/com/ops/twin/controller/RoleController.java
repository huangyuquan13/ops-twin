package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.audit.AuditLog;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AuditEvent;
import com.ops.twin.entity.SysRole;
import com.ops.twin.entity.SysRolePermission;
import com.ops.twin.service.AuditEventService;
import com.ops.twin.service.SysRoleService;
import com.ops.twin.mapper.SysRolePermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 * 接口前缀：/api/system/role
 */
@RestController
@RequestMapping("/api/system/role")
public class RoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Autowired
    private AuditEventService auditEventService;

    /** 角色列表 */
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(sysRoleService.list());
    }

    /** 保存角色（新增或更新） */
    @AuditLog(operation = "CREATE_ROLE", description = "新增角色")
    @PostMapping("/save")
    public Result<SysRole> save(@RequestBody SysRole role) {
        sysRoleService.saveOrUpdate(role);
        return Result.success(role);
    }

    /** 删除角色 */
    @AuditLog(operation = "DELETE_ROLE", description = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 删除关联的权限映射
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
        );
        return sysRoleService.removeById(id)
            ? Result.success(true) : Result.error("删除失败");
    }

    /** 获取角色的权限 ID 列表 */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        List<Long> permIds = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
        ).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permIds);
    }

    /** 保存角色权限 */
    @PostMapping("/{id}/permissions")
    public Result<Boolean> savePermissions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("permissionIds");
        if (raw == null) return Result.error("缺少 permissionIds 参数");
        List<Long> permIds = raw.stream().map(Number::longValue).collect(Collectors.toList());

        // 先删旧映射
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
        );
        // 再插新映射
        for (Long permId : permIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permId);
            sysRolePermissionMapper.insert(rp);
        }
        // 记录审计
        SysRole role = sysRoleService.getById(id);
        AuditEvent audit = new AuditEvent();
        audit.setOperator("admin");
        audit.setEventType("UPDATE_ROLE_PERM");
        audit.setDetail("修改角色权限【" + (role != null ? role.getRoleName() : "id=" + id) + "】，分配 " + permIds.size() + " 项权限");
        auditEventService.save(audit);

        return Result.success(true);
    }
}
