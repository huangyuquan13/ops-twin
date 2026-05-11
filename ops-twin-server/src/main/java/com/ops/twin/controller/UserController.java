package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.audit.AuditLog;
import com.ops.twin.common.Result;
import com.ops.twin.entity.SysUser;
import com.ops.twin.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/user")
public class UserController {

    @Autowired
    private SysUserMapper sysUserMapper;

    // 1. 头像上传接口
    @PostMapping("/upload")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            // 获取项目根目录下的 uploads 文件夹
            String uploadDir = new File("uploads").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 自动创建文件夹
            }
            
            // 生成唯一的文件名，防止重名覆盖
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + extension;
            
            // 保存文件到本地
            File dest = new File(dir, newFileName);
            file.transferTo(dest);
            
            // 返回可供前端访问的 URL (配合 WebConfig)
            String avatarUrl = "/uploads/" + newFileName;
            return Result.success(avatarUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    // 2. 分页查询用户
    @GetMapping("/list")
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username) {
        
        Page<SysUser> page = new Page<>(current, size);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        
        if (username != null && !username.isEmpty()) {
            wrapper.like("username", username);
        }
        // 按照创建时间倒序
        wrapper.orderByDesc("create_time");
        
        return Result.success(sysUserMapper.selectPage(page, wrapper));
    }

    // 3. 保存或更新用户
    @AuditLog(operation = "CREATE_USER", description = "新增用户")
    @PostMapping("/save")
    public Result<String> save(@RequestBody SysUser user) {
        // 如果密码不为空，说明用户输入了新密码，需要进行加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String md5Pwd = org.springframework.util.DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
            user.setPassword(md5Pwd);
        }

        if (user.getId() == null) {
            // 新增用户逻辑
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                // 如果没填密码，给一个默认加密后的 123456
                user.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex("123456".getBytes()));
            }
            user.setCreateTime(LocalDateTime.now());
            sysUserMapper.insert(user);
        } else {
            // 更新用户逻辑
            sysUserMapper.updateById(user);
        }
        return Result.success("保存成功");
    }

    // 4. 删除用户
    @AuditLog(operation = "DELETE_USER", description = "删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        sysUserMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
