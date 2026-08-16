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
public class UserChatHandler extends TextWebSocketHandler {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getSubjectId(session);
        String token = getToken(session);
        if (userId == null) {
            session.close(new CloseStatus(4002, "unauthorized"));
            return;
        }

        ChatConnection connection = new ChatConnection(session, token, userId, SubjectType.USER);
        connectionPool.put(SubjectType.USER, userId, connection);
        log.info("User {} connected via WS, pool size: {}", userId, connectionPool.sizeOf(SubjectType.USER));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getSubjectId(session);
        if (userId == null) {
            session.close(new CloseStatus(4002, "unauthorized"));
            return;
        }

        try {
            ChatMessage request = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            ChatMessage response = chatService.handleUserMessage(userId, request);
            if (response != null) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        } catch (Exception e) {
            log.error("Error handling message from user {}", userId, e);
            ChatMessage error = ChatMessage.builder()
                    .state(ChatMessage.State.ERROR)
                    .content("internal error")
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getSubjectId(session);
        if (userId != null) {
            connectionPool.remove(SubjectType.USER, userId);
            log.info("User {} disconnected, reason: {}, pool size: {}", userId, status.getReason(),
                    connectionPool.sizeOf(SubjectType.USER));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getSubjectId(session);
        log.error("WS transport error for user {}", userId, exception);
        if (userId != null) {
            connectionPool.remove(SubjectType.USER, userId);
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
