package com.ops.twin.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket 处理器：实时日志推送
 * 路径模式：/ws/task/log/{recordId}
 * 职责：管理所有在线会话，并提供向指定 recordId 频道广播日志的能力
 */
@Slf4j
@Component
public class TaskLogWebSocketHandler extends TextWebSocketHandler {

    /**
     * 维护 recordId -> 所有订阅该任务的 WebSocket 会话列表
     * ConcurrentHashMap + CopyOnWriteArrayList 保证并发安全
     */
    private static final Map<String, CopyOnWriteArrayList<WebSocketSession>> SESSION_MAP
            = new ConcurrentHashMap<>();

    /**
     * 客户端建立连接时，从 URL 中解析 recordId 并注册到订阅列表
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String recordId = extractRecordId(session);
        SESSION_MAP.computeIfAbsent(recordId, k -> new CopyOnWriteArrayList<>()).add(session);
        log.info("[WebSocket] 客户端连接：recordId={}, sessionId={}", recordId, session.getId());
    }

    /**
     * 客户端断开连接时，从订阅列表中移除，避免内存泄漏
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String recordId = extractRecordId(session);
        CopyOnWriteArrayList<WebSocketSession> sessions = SESSION_MAP.get(recordId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                // 订阅者全部离线，清理该 recordId 的 Map 条目
                SESSION_MAP.remove(recordId);
            }
        }
        log.info("[WebSocket] 客户端断开：recordId={}, sessionId={}", recordId, session.getId());
    }

    /**
     * 向指定 recordId 的所有在线客户端广播日志消息
     * 供演练引擎调用
     *
     * @param recordId 任务记录 ID
     * @param message  日志消息
     */
    public void broadcast(String recordId, String message) {
        CopyOnWriteArrayList<WebSocketSession> sessions = SESSION_MAP.get(recordId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    // 同步发送，确保消息顺序
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("[WebSocket] 发送消息失败：sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 从 WebSocket 会话的 URI 中提取 recordId
     * 示例 URI：ws://localhost:8080/ws/task/log/42
     */
    private String extractRecordId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        // 路径格式：/ws/task/log/{recordId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }
}
