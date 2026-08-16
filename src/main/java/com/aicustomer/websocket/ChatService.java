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

/**
 * WebSocket 聊天消息处理核心服务。
 * 负责：会话创建/复用、消息落库、会话时间戳更新、实时转发。
 * 不负责连接管理（由 ChatHandler 负责），也不负责连接存活判断（由定时任务负责）。
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    /** 复用 Spring 容器里配置好的单例 ObjectMapper，而不是每次转发都 new 一个 */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理用户发来的消息。
     *
     * 流程：
     * 1. 校验 content 非空
     * 2. 有 sessionId 则复用既有会话（归属按 sessionId 查，不采信消息体里的 goodsId/ctId）
     *    无 sessionId 则按 userId+ctId+goodsId 查既有会话，查不到则新建
     * 3. 消息落库
     * 4. 更新会话 lastMessageTime
     * 5. 实时转发给对应商户（不在线则静默丢弃，不影响落库成功）
     * 6. 回执给发送方（SESSION_CREATED 或 SUCCESS）
     */
    @Transactional
    public ChatMessage handleUserMessage(Long userId, ChatMessage request) {
        // 步骤1：校验必填字段
        if (isBlank(request.getContent())) {
            return buildError("content is required");
        }

        // 步骤2：查找或创建会话
        boolean sessionCreated = request.getSessionId() == null;
        Session session = sessionCreated
                ? findOrCreateSessionForUser(userId, request)
                : findExistingSessionForUser(userId, request.getSessionId());
        if (session == null) {
            return buildError(sessionCreated
                    ? "goodsId and ctId are required to start a session"
                    : "session not found or not accessible");
        }

        // 步骤3：生成消息 ID 和时间戳
        Instant timestamp = Instant.now();
        String messageId = resolveMessageId(request);

        // 步骤4：消息落库（userId -> ctId）
        persistMessage(messageId, session.getId(), userId, SubjectType.USER,
                session.getCtId(), SubjectType.TENANT, request.getContent());
        touchSession(session, timestamp);

        // 步骤5：实时转发给商户
        forwardMessage(SubjectType.TENANT, session.getCtId(), SubjectType.USER, userId,
                session.getId(), session.getGoodsId(), request.getContent(), messageId, timestamp);

        // 步骤6：回执给发送方
        return buildReply(messageId, session.getId(), timestamp,
                sessionCreated ? ChatMessage.State.SESSION_CREATED : ChatMessage.State.SUCCESS);
    }

    /**
     * 处理商户发来的消息。
     *
     * 流程：
     * 1. 校验 sessionId 和 content 非空
     * 2. 按 sessionId 查找会话并校验归属（只有该商户才能回复自己名下的会话）
     * 3. 消息落库
     * 4. 更新会话 lastMessageTime
     * 5. 实时转发给对应用户（不在线则静默丢弃）
     * 6. 回执 SUCCESS 给发送方
     */
    @Transactional
    public ChatMessage handleTenantMessage(Long ctId, ChatMessage request) {
        // 步骤1：校验必填字段
        if (request.getSessionId() == null || isBlank(request.getContent())) {
            return buildError("sessionId and content are required");
        }

        // 步骤2：查找会话并校验归属
        Session session = sessionRepository.findById(request.getSessionId()).orElse(null);
        if (session == null || !session.getCtId().equals(ctId)) {
            return buildError("session not found or not accessible");
        }

        // 步骤3：生成消息 ID 和时间戳
        Instant timestamp = Instant.now();
        String messageId = resolveMessageId(request);

        // 步骤4：消息落库（ctId -> userId）
        persistMessage(messageId, session.getId(), ctId, SubjectType.TENANT,
                session.getUserId(), SubjectType.USER, request.getContent());
        touchSession(session, timestamp);

        // 步骤5：实时转发给用户
        forwardMessage(SubjectType.USER, session.getUserId(), SubjectType.TENANT, ctId,
                session.getId(), null, request.getContent(), messageId, timestamp);

        // 步骤6：回执给发送方
        return buildReply(messageId, session.getId(), timestamp, ChatMessage.State.SUCCESS);
    }

    // ---------------------------------------------------------------
    // 会话查找 / 创建
    // ---------------------------------------------------------------

    /**
     * 首条消息：找到 userId+ctId+goodsId 对应的既有会话，没有就新建一个。
     * goodsId 或 ctId 任一为空时返回 null（上层会返回错误回执）。
     */
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

    /**
     * 已有会话：只信任 sessionId 对应的历史归属，不采信消息体里的 goodsId/ctId。
     * 只返回 userId 完全匹配的历史会话，防止跨用户越权。
     */
    private Session findExistingSessionForUser(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        return (session != null && session.getUserId().equals(userId)) ? session : null;
    }

    // ---------------------------------------------------------------
    // 消息落库 / 会话时间戳更新
    // ---------------------------------------------------------------

    /** 优先用请求里已有的 messageId，为空则生成一个新的 UUID（幂等发送场景会用到） */
    private String resolveMessageId(ChatMessage request) {
        return isBlank(request.getMessageId()) ? UUID.randomUUID().toString() : request.getMessageId();
    }

    /** 将消息写入 MySQL，若写入失败则向上层抛异常（触发事务回滚） */
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

    /**
     * 新消息到达时同步会话的 lastMessageTime。
     * 否则收件箱列表的时间/排序会一直停在会话创建那一刻。
     */
    private void touchSession(Session session, Instant timestamp) {
        session.setLastMessageTime(timestamp);
        sessionRepository.save(session);
    }

    // ---------------------------------------------------------------
    // 回执构建 / 实时转发
    // ---------------------------------------------------------------

    /** 构建发给发送方的回执消息（不含 content） */
    private ChatMessage buildReply(String messageId, Long sessionId, Instant timestamp, ChatMessage.State state) {
        return ChatMessage.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .state(state)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 把消息实时推送给对方（用户->商户 或 商户->用户 共用同一套逻辑）。
     * goodsId 只有"转发给商户"时有意义，转发给用户时传 null 即可
     *（ChatMessage 上没有该字段，@JsonInclude(NON_NULL) 会自动跳过）。
     * 若对方不在线则静默丢弃——此时消息已落库，丢弃的只是实时推送。
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

    /** 构建错误回执 */
    private ChatMessage buildError(String message) {
        return ChatMessage.builder()
                .state(ChatMessage.State.ERROR)
                .content(message)
                .build();
    }
}
