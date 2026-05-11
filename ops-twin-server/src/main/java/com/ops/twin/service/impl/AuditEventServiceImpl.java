package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.AuditEvent;
import com.ops.twin.mapper.AuditEventMapper;
import com.ops.twin.service.AuditEventService;
import org.springframework.stereotype.Service;

@Service
public class AuditEventServiceImpl extends ServiceImpl<AuditEventMapper, AuditEvent>
        implements AuditEventService {
}
