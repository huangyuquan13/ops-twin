package com.ops.twin.controller;

import com.ops.twin.common.Result;
import com.ops.twin.entity.SysUser;
import com.ops.twin.mapper.SysUserMapper;
import com.ops.twin.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginUser) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginUser.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // Try bcrypt first, then MD5 fallback for not-yet-migrated passwords
        boolean matched = false;
        String storedPassword = user.getPassword();
        try {
            matched = passwordEncoder.matches(loginUser.getPassword(), storedPassword);
        } catch (Exception ignored) {
            // not a bcrypt hash
        }
        if (!matched) {
            String md5 = org.springframework.util.DigestUtils.md5DigestAsHex(
                    loginUser.getPassword().getBytes());
            if (storedPassword.equalsIgnoreCase(md5)
                    || storedPassword.equals(loginUser.getPassword())) {
                matched = true;
                // Auto-migrate to bcrypt on successful MD5 login
                user.setPassword(passwordEncoder.encode(loginUser.getPassword()));
                sysUserMapper.updateById(user);
            }
        }

        if (!matched) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getRoleId());
        user.setPassword(null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }
}
