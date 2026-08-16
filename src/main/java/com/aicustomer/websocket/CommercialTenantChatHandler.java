package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.websocket.message.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 商户端 WebSocket 处理器，负责商户 WS 连接的建立、消息收发和断开。
 * 连接建立前的身份鉴权由 ChatHandshakeInterceptor 在握手阶段完成，
 * 成功后把 subjectId/token 注入 session attributes，本 Handler 直接取用。
 * 逻辑与 UserChatHandler 完全对称，区别在于调用 ChatService.handleTenantMessage。
 */
@Slf4j
@Component
public class CommercialTenantChatHandler extends TextWebSocketHandler {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * WS 连接建立成功回调。
     *
     * 步骤：
     * 1. 从 session attributes 取鉴权得到的 ctId 和 token
     * 2. ctId 为空表示握手阶段被拦截，直接拒绝连接
     * 3. 将连接放入连接池（新连接入池时会自动关闭该身份的旧连接）
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long ctId = getSubjectId(session);
        String token = getToken(session);
        if (ctId == null) {
            session.close(new CloseStatus(4002, "unauthorized"));
            return;
        }

        ChatConnection connection = new ChatConnection(session, token, ctId, SubjectType.TENANT);
        connectionPool.put(SubjectType.TENANT, ctId, connection);
        log.info("Tenant {} connected via WS, pool size: {}", ctId, connectionPool.sizeOf(SubjectType.TENANT));
    }

    /**
     * 处理商户发来的文本消息。
     *
     * 步骤：
     * 1. 校验 ctId 存在（握手阶段已鉴权，此处防御性校验）
     * 2. JSON 反序列化消息体
     * 3. 调用 ChatService 处理消息，获得回执
     * 4. 将回执通过当前 WS 连接发回给商户
     * 5. 处理过程中的异常统一返回 ERROR 回执
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long ctId = getSubjectId(session);
        if (ctId == null) {
            session.close(new CloseStatus(4002, "unauthorized"));
            return;
        }

        try {
            ChatMessage request = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            ChatMessage response = chatService.handleTenantMessage(ctId, request);
            if (response != null) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        } catch (Exception e) {
            log.error("Error handling message from tenant {}", ctId, e);
            ChatMessage error = ChatMessage.builder()
                    .state(ChatMessage.State.ERROR)
                    .content("internal error")
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }

    /**
     * WS 连接正常或异常关闭回调。
     *
     * 步骤：
     * 1. 从 session attributes 取 ctId
     * 2. 从连接池中移除该连接并记录日志
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long ctId = getSubjectId(session);
        if (ctId != null) {
            connectionPool.remove(SubjectType.TENANT, ctId);
            log.info("Tenant {} disconnected, reason: {}, pool size: {}", ctId, status.getReason(),
                    connectionPool.sizeOf(SubjectType.TENANT));
        }
    }

    /**
     * WS 传输层错误回调（网络中断等）。
     * 区别于 afterConnectionClosed：此处只处理底层异常，主动从连接池清理。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long ctId = getSubjectId(session);
        log.error("WS transport error for tenant {}", ctId, exception);
        if (ctId != null) {
            connectionPool.remove(SubjectType.TENANT, ctId);
        }
    }

    /** 从 session attributes 中获取握手阶段注入的 subjectId（Long 类型） */
    private Long getSubjectId(WebSocketSession session) {
        Object id = session.getAttributes().get("subjectId");
        return id instanceof Long ? (Long) id : null;
    }

    /** 从 session attributes 中获取握手阶段注入的 token（String 类型） */
    private String getToken(WebSocketSession session) {
        Object token = session.getAttributes().get("token");
        return token instanceof String ? (String) token : null;
    }
}
