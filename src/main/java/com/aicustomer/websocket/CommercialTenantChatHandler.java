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

@Slf4j
@Component
public class CommercialTenantChatHandler extends TextWebSocketHandler {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long ctId = getSubjectId(session);
        if (ctId != null) {
            connectionPool.remove(SubjectType.TENANT, ctId);
            log.info("Tenant {} disconnected, reason: {}, pool size: {}", ctId, status.getReason(),
                    connectionPool.sizeOf(SubjectType.TENANT));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long ctId = getSubjectId(session);
        log.error("WS transport error for tenant {}", ctId, exception);
        if (ctId != null) {
            connectionPool.remove(SubjectType.TENANT, ctId);
        }
    }

    private Long getSubjectId(WebSocketSession session) {
        Object id = session.getAttributes().get("subjectId");
        return id instanceof Long ? (Long) id : null;
    }

    private String getToken(WebSocketSession session) {
        Object token = session.getAttributes().get("token");
        return token instanceof String ? (String) token : null;
    }
}
