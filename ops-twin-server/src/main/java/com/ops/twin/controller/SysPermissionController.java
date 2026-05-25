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
@RequestMapping("/api/system/permission")
public class SysPermissionController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    /** 保存权限节点（新增或更新） */
    @PostMapping("/save")
    public Result<SysPermission> save(@RequestBody SysPermission perm) {
        if (perm.getId() != null) {
            sysPermissionMapper.updateById(perm);
        } else {
            sysPermissionMapper.insert(perm);
        }
        return Result.success(perm);
    }

    /** 删除权限节点（含所有子节点） */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 收集所有要删的节点 ID（自身 + 子孙）
        Set<Long> idsToDelete = new HashSet<>();
        idsToDelete.add(id);
        collectChildren(id, idsToDelete);

        // 删除角色-权限关联
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getPermissionId, idsToDelete)
        );
        // 删除权限节点
        sysPermissionMapper.deleteBatchIds(idsToDelete);
        return Result.success(true);
    }

    private void collectChildren(Long parentId, Set<Long> ids) {
        List<SysPermission> children = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, parentId)
        );
        for (SysPermission child : children) {
            ids.add(child.getId());
            collectChildren(child.getId(), ids);
        }
    }
}
