package com.ops.twin.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dashboard 3D 实时事件推送
 * 路径：/ws/dashboard/events
 * 演练引擎执行时通过 broadcast() 推送主机状态变更，3D 大屏实时变色
 */
@Slf4j
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final CopyOnWriteArrayList<WebSocketSession> SESSIONS = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSIONS.add(session);
        log.info("[Dashboard-WS] 3D 大屏已连接, sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.remove(session);
        log.info("[Dashboard-WS] 3D 大屏已断开, sessionId={}", session.getId());
    }

    /** 向所有 3D 大屏客户端广播事件 */
    public static void broadcast(String json) {
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : SESSIONS) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(msg);
                    }
                } catch (IOException e) {
                    log.warn("[Dashboard-WS] 发送失败: {}", e.getMessage());
                }
            }
        }
    }
}
