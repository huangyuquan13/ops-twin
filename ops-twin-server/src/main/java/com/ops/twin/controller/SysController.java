package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.common.Result;
import com.ops.twin.entity.SysPermission;
import com.ops.twin.entity.SysRolePermission;
import com.ops.twin.mapper.SysPermissionMapper;
import com.ops.twin.mapper.SysRolePermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
public class SysController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * 动态菜单 — 根据角色返回
     * GET /api/system/menus?roleId=1
     * 返回: { menus: [...], permissions: ["strategy:add", ...] }
     */
    @GetMapping("/menus")
    public Result<Map<String, Object>> getMenus(@RequestParam(defaultValue = "1") Long roleId) {
        // 1. 查出该角色拥有的权限 ID
        List<Long> permIds = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId)
        ).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());

        if (permIds.isEmpty()) {
            return Result.success(Map.of("menus", List.of(), "permissions", List.of()));
        }

        // 2. 查出这些权限的完整信息
        List<SysPermission> all = sysPermissionMapper.selectBatchIds(permIds);

        // 3. 分离菜单和按钮
        List<SysPermission> menus = all.stream()
            .filter(p -> p.getType() == 1)
            .sorted(Comparator.comparing(p -> p.getSort() != null ? p.getSort() : 0))
            .collect(Collectors.toList());

        List<String> permissions = all.stream()
            .filter(p -> p.getType() == 2 && p.getPermissionCode() != null)
            .map(SysPermission::getPermissionCode)
            .collect(Collectors.toList());

        return Result.success(Map.of("menus", menus, "permissions", permissions));
    }

    /** 返回全量权限列表（供权限管理页构建树） */
    @GetMapping("/permissions/all")
    public Result<List<SysPermission>> getAllPermissions() {
        return Result.success(sysPermissionMapper.selectList(null));
    }
}
