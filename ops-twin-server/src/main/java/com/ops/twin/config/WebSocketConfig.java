package com.ops.twin.config;

import com.ops.twin.websocket.DashboardWebSocketHandler;
import com.ops.twin.websocket.TaskLogWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 总配置类
 * /ws/task/log/{recordId} — 演练终端实时日志
 * /ws/dashboard/events    — 3D 大屏实时状态推送
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TaskLogWebSocketHandler taskLogWebSocketHandler;

    @Autowired
    private DashboardWebSocketHandler dashboardWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(taskLogWebSocketHandler, "/ws/task/log/{recordId}")
            .setAllowedOrigins("*");

        registry
            .addHandler(dashboardWebSocketHandler, "/ws/dashboard/events")
            .setAllowedOrigins("*");
    }
}
