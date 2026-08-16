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
import org.springframework.web.socket.TextMessage;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    // 复用 Spring 容器里配置好的单例 ObjectMapper，而不是每次转发都 new 一个
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ChatMessage handleUserMessage(Long userId, ChatMessage request) {
        if (isBlank(request.getContent())) {
            return buildError("content is required");
        }

        boolean sessionCreated = request.getSessionId() == null;
        Session session = sessionCreated
                ? findOrCreateSessionForUser(userId, request)
                : findExistingSessionForUser(userId, request.getSessionId());
        if (session == null) {
            return buildError(sessionCreated
                    ? "goodsId and ctId are required to start a session"
                    : "session not found or not accessible");
        }

        Instant timestamp = Instant.now();
        String messageId = resolveMessageId(request);

        persistMessage(messageId, session.getId(), userId, SubjectType.USER,
                session.getCtId(), SubjectType.TENANT, request.getContent());
        touchSession(session, timestamp);

        forwardMessage(SubjectType.TENANT, session.getCtId(), SubjectType.USER, userId,
                session.getId(), session.getGoodsId(), request.getContent(), messageId, timestamp);

        return buildReply(messageId, session.getId(), timestamp,
                sessionCreated ? ChatMessage.State.SESSION_CREATED : ChatMessage.State.SUCCESS);
    }

    @Transactional
    public ChatMessage handleTenantMessage(Long ctId, ChatMessage request) {
        if (request.getSessionId() == null || isBlank(request.getContent())) {
            return buildError("sessionId and content are required");
        }

        Session session = sessionRepository.findById(request.getSessionId()).orElse(null);
        if (session == null || !session.getCtId().equals(ctId)) {
            return buildError("session not found or not accessible");
        }

        Instant timestamp = Instant.now();
        String messageId = resolveMessageId(request);

        persistMessage(messageId, session.getId(), ctId, SubjectType.TENANT,
                session.getUserId(), SubjectType.USER, request.getContent());
        touchSession(session, timestamp);

        forwardMessage(SubjectType.USER, session.getUserId(), SubjectType.TENANT, ctId,
                session.getId(), null, request.getContent(), messageId, timestamp);

        return buildReply(messageId, session.getId(), timestamp, ChatMessage.State.SUCCESS);
    }

    // ---------------------------------------------------------------
    // 会话查找/创建
    // ---------------------------------------------------------------

    /** 首条消息：找到用户+商户+商品对应的既有会话，没有就新建一个 */
    private Session findOrCreateSessionForUser(Long userId, ChatMessage request) {
        if (request.getGoodsId() == null || request.getCtId() == null) {
            return null;
        }
        return sessionRepository.findByUserIdAndCtIdAndGoodsId(userId, request.getCtId(), request.getGoodsId())
                .orElseGet(() -> sessionRepository.save(
                        Session.builder()
                                .userId(userId)
                                .ctId(request.getCtId())
                                .goodsId(request.getGoodsId())
                                .conversationStatus(Session.ConversationStatus.ACTIVE)
                                .lastMessageTime(Instant.now())
                                .build()
                ));
    }

    /** 已有会话：只信任 sessionId 对应的历史归属，不采信消息体里的 goodsId/ctId */
    private Session findExistingSessionForUser(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        return (session != null && session.getUserId().equals(userId)) ? session : null;
    }

    // ---------------------------------------------------------------
    // 消息落库 / 会话时间戳
    // ---------------------------------------------------------------

    private String resolveMessageId(ChatMessage request) {
        return isBlank(request.getMessageId()) ? UUID.randomUUID().toString() : request.getMessageId();
    }

    private void persistMessage(String messageId, Long sessionId, Long senderId, SubjectType senderType,
                                Long receiverId, SubjectType receiverType, String content) {
        Message message = Message.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .senderId(senderId)
                .senderType(senderType)
                .receiverId(receiverId)
                .receiverType(receiverType)
                .content(content)
                .isRead(false)
                .build();
        messageRepository.save(message);
    }

    /** 新消息到达时同步会话的 lastMessageTime，否则收件箱列表的时间/排序会一直停在会话创建那一刻 */
    private void touchSession(Session session, Instant timestamp) {
        session.setLastMessageTime(timestamp);
        sessionRepository.save(session);
    }

    // ---------------------------------------------------------------
    // 回执 / 实时转发
    // ---------------------------------------------------------------

    private ChatMessage buildReply(String messageId, Long sessionId, Instant timestamp, ChatMessage.State state) {
        return ChatMessage.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .state(state)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 把消息实时推给对方（用户->商户 或 商户->用户 共用同一套逻辑）。
     * goodsId 只有"转发给商户"时有意义，转发给用户时传 null 即可（ChatMessage 上没有该字段不会序列化，见 @JsonInclude(NON_NULL)）。
     */
    private void forwardMessage(SubjectType targetType, Long targetId, SubjectType senderType, Long senderId,
                                Long sessionId, Long goodsId, String content, String messageId, Instant timestamp) {
        ChatConnection conn = connectionPool.get(targetType, targetId);
        if (conn == null) {
            log.debug("{} {} not connected, message queued (not implemented)", targetType, targetId);
            return;
        }
        try {
            ChatMessage forwarded = ChatMessage.builder()
                    .messageId(messageId)
                    .state(ChatMessage.State.DELIVERED)
                    .content(content)
                    .sessionId(sessionId)
                    .senderId(senderId)
                    .senderType(senderType)
                    .receiverId(targetId)
                    .receiverType(targetType)
                    .goodsId(goodsId)
                    .timestamp(timestamp)
                    .build();
            conn.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(forwarded)));
        } catch (Exception e) {
            log.error("Failed to forward message to {} {}", targetType, targetId, e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ChatMessage buildError(String message) {
        return ChatMessage.builder()
                .state(ChatMessage.State.ERROR)
                .content(message)
                .build();
    }
}