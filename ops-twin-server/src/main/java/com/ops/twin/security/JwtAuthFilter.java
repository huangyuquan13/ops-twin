package com.ops.twin.security;

import com.alibaba.fastjson2.JSON;
import com.ops.twin.common.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 登录接口 + WebSocket + 静态资源不需要 JWT 校验
        if (path.equals("/api/auth/login") || path.startsWith("/ws/") || path.startsWith("/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            sendError(response, 401, "未登录");
            return;
        }

        String token = header.substring(7);
        if (!jwtUtils.validateToken(token)) {
            sendError(response, 401, "token无效或已过期");
            return;
        }

        // 把 JWT 中的用户信息写入 Spring Security 上下文，否则 Security 认为未认证返回 403
        Claims claims = jwtUtils.parseToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(Result.error(msg)));
    }
}
