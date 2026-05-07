package com.ops.twin.controller;

import com.ops.twin.common.Result;
import com.ops.twin.entity.SysPermission;
import com.ops.twin.mapper.SysPermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class SysController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @GetMapping("/menus")
    public Result<List<SysPermission>> getMenus() {
        // 返回全量菜单，前端根据这个构建侧边栏
        return Result.success(sysPermissionMapper.selectList(null));
    }
}
