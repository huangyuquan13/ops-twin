package com.ops.twin.config;

import com.ops.twin.websocket.TaskLogWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 总配置类
 * 注册实时日志推送端点：/ws/task/log/{recordId}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TaskLogWebSocketHandler taskLogWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            // 注册日志推送 Handler，允许所有来源（开发阶段）
            .addHandler(taskLogWebSocketHandler, "/ws/task/log/{recordId}")
            .setAllowedOrigins("*");
    }
}
