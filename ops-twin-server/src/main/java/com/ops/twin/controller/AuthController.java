package com.ops.twin.controller;

import com.ops.twin.common.Result;
import com.ops.twin.entity.SysUser;
import com.ops.twin.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.util.DigestUtils;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginUser) {

        // 去数据库查这个真实的用户名
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginUser.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user != null) {
            // 前端传来的是明文 "123456"，我们需要转成 MD5 才能和数据库里的密文比对
            String md5Password = DigestUtils.md5DigestAsHex(loginUser.getPassword().getBytes());
            
            // 为了防止数据库里的密码有大写，统一转小写比对，或者有些老数据可能直接存的明文，做一个兼容
            if (user.getPassword().equalsIgnoreCase(md5Password) || user.getPassword().equals(loginUser.getPassword())) {
                Map<String, Object> data = new HashMap<>();
                data.put("token", "mock-jwt-token-123456");
                data.put("user", user); // 把包含头像的真实用户信息带回给前端
                return Result.success(data);
            }
        }
        return Result.error("用户名或密码错误");
    }
}
