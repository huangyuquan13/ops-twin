package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.SysRole;
import com.ops.twin.mapper.SysRoleMapper;
import com.ops.twin.service.SysRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {
}
