package com.ops.twin.audit;

import com.ops.twin.entity.AuditEvent;
import com.ops.twin.mapper.AuditEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditEventMapper auditEventMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String operator = "unknown";
        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                operator = "authenticated";
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
            saveAudit(operator, auditLog.operation(), auditLog.description(), "SUCCESS");
        } catch (Throwable e) {
            saveAudit(operator, auditLog.operation(), auditLog.description(), "FAILED: " + e.getMessage());
            throw e;
        }
        return result;
    }

    private void saveAudit(String operator, String eventType, String detail, String status) {
        AuditEvent event = new AuditEvent();
        event.setOperator(operator);
        event.setEventType(eventType);
        event.setDetail(detail + " [" + status + "]");
        event.setCreateTime(LocalDateTime.now());
        auditEventMapper.insert(event);
    }
}
