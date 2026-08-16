package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.entity.Message;
import com.aicustomer.entity.Session;
import com.aicustomer.repository.MessageRepository;
import com.aicustomer.repository.SessionRepository;
import com.aicustomer.websocket.message.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class ChatService {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Transactional
    public ChatMessage handleUserMessage(Long userId, ChatMessage request) {
        if (request.getGoodsId() == null || request.getContent() == null || request.getContent().isBlank()) {
            return buildError("goodsId and content are required");
        }
        if (request.getCtId() == null) {
            return buildError("ctId is required for user message");
        }

        Long sessionId = request.getSessionId();
        boolean sessionCreated = false;

        if (sessionId == null) {
            Session session = sessionRepository.findByUserIdAndCtIdAndGoodsId(
                    userId, request.getCtId(), request.getGoodsId()
            ).orElseGet(() -> {
                Session newSession = Session.builder()
                        .userId(userId)
                        .ctId(request.getCtId())
                        .goodsId(request.getGoodsId())
                        .conversationStatus(Session.ConversationStatus.ACTIVE)
                        .lastMessageTime(Instant.now())
                        .build();
                return sessionRepository.save(newSession);
            });
            sessionId = session.getId();
            sessionCreated = true;
        }

        String messageId = request.getMessageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = java.util.UUID.randomUUID().toString();
        }
        Instant timestamp = Instant.now();

        Message message = Message.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .senderId(userId)
                .senderType(SubjectType.USER)
                .receiverId(request.getCtId())
                .receiverType(SubjectType.TENANT)
                .content(request.getContent())
                .isRead(false)
                .build();
        messageRepository.save(message);

        ChatMessage reply = ChatMessage.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .state(sessionCreated ? ChatMessage.State.SESSION_CREATED : ChatMessage.State.SUCCESS)
                .timestamp(timestamp)
                .build();

        forwardToTenant(request.getCtId(), request, messageId, sessionId, userId, timestamp);

        return reply;
    }

    @Transactional
    public ChatMessage handleTenantMessage(Long ctId, ChatMessage request) {
        if (request.getSessionId() == null || request.getContent() == null || request.getContent().isBlank()) {
            return buildError("sessionId and content are required");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElse(null);
        if (session == null || !session.getCtId().equals(ctId)) {
            return buildError("session not found or not accessible");
        }

        String messageId = request.getMessageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = java.util.UUID.randomUUID().toString();
        }
        Instant timestamp = Instant.now();

        Message message = Message.builder()
                .messageId(messageId)
                .sessionId(request.getSessionId())
                .senderId(ctId)
                .senderType(SubjectType.TENANT)
                .receiverId(session.getUserId())
                .receiverType(SubjectType.USER)
                .content(request.getContent())
                .isRead(false)
                .build();
        messageRepository.save(message);

        ChatMessage reply = ChatMessage.builder()
                .messageId(messageId)
                .sessionId(request.getSessionId())
                .state(ChatMessage.State.SUCCESS)
                .timestamp(timestamp)
                .build();

        forwardToUser(session.getUserId(), request, messageId, request.getSessionId(), ctId, timestamp);

        return reply;
    }

    private void forwardToTenant(Long ctId, ChatMessage original, String messageId,
                                 Long sessionId, Long senderId, Instant timestamp) {
        ChatConnection conn = connectionPool.get(SubjectType.TENANT, ctId);
        if (conn == null) {
            log.debug("Tenant {} not connected, message queued (not implemented)", ctId);
            return;
        }
        try {
            ChatMessage forwarded = ChatMessage.builder()
                    .messageId(messageId)
                    .state(ChatMessage.State.DELIVERED)
                    .content(original.getContent())
                    .sessionId(sessionId)
                    .senderId(senderId)
                    .senderType(SubjectType.USER)
                    .receiverId(ctId)
                    .receiverType(SubjectType.TENANT)
                    .goodsId(original.getGoodsId())
                    .timestamp(timestamp)
                    .build();
            conn.getSession().sendMessage(new org.springframework.web.socket.TextMessage(new ObjectMapper().writeValueAsString(forwarded)));
        } catch (Exception e) {
            log.error("Failed to forward message to tenant {}", ctId, e);
        }
    }

    private void forwardToUser(Long userId, ChatMessage original, String messageId,
                               Long sessionId, Long senderId, Instant timestamp) {
        ChatConnection conn = connectionPool.get(SubjectType.USER, userId);
        if (conn == null) {
            log.debug("User {} not connected, message queued (not implemented)", userId);
            return;
        }
        try {
            ChatMessage forwarded = ChatMessage.builder()
                    .messageId(messageId)
                    .state(ChatMessage.State.DELIVERED)
                    .content(original.getContent())
                    .sessionId(sessionId)
                    .senderId(senderId)
                    .senderType(SubjectType.TENANT)
                    .receiverId(userId)
                    .receiverType(SubjectType.USER)
                    .timestamp(timestamp)
                    .build();
            conn.getSession().sendMessage(new org.springframework.web.socket.TextMessage(new ObjectMapper().writeValueAsString(forwarded)));
        } catch (Exception e) {
            log.error("Failed to forward message to user {}", userId, e);
        }
    }

    private ChatMessage buildError(String message) {
        return ChatMessage.builder()
                .state(ChatMessage.State.ERROR)
                .content(message)
                .build();
    }
}
