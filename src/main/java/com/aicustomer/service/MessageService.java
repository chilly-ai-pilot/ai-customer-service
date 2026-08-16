package com.aicustomer.service;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.dto.response.MessageResponse;
import com.aicustomer.entity.Message;
import com.aicustomer.entity.Session;
import com.aicustomer.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 消息服务，提供消息列表查询、已读标记、未读数统计。
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 查询会话消息列表（按时间正序）。
     *
     * @param session  已校验归属的会话实体
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页消息列表
     */
    public Page<MessageResponse> listMessages(Session session, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> page = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId(), pageRequest);
        return page.map(this::toResponse);
    }

    /**
     * 标记会话消息为已读。
     *
     * @param session     已校验归属的会话实体
     * @param currentUser 当前登录用户
     * @return 标记结果（含会话 ID、标记条数、标记时间）
     */
    @Transactional
    public java.util.Map<String, Object> markRead(Session session, CurrentUser currentUser) {
        Instant readAt = Instant.now();
        int count = messageRepository.markReadBySessionAndReceiver(
                session.getId(), currentUser.getId(), currentUser.getType(), readAt);
        return java.util.Map.of(
                "sessionId", session.getId(),
                "markedCount", count,
                "readAt", readAt
        );
    }

    /**
     * 统计当前用户在指定会话中的未读消息数。
     *
     * @param session     已校验归属的会话实体
     * @param currentUser 当前登录用户
     * @return 未读消息数
     */
    public long countUnread(Session session, CurrentUser currentUser) {
        return messageRepository.countUnreadBySessionAndReceiver(
                session.getId(), currentUser.getId(), currentUser.getType());
    }

    /** 将 Message 实体转为响应 DTO */
    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .messageId(message.getMessageId())
                .sessionId(message.getSessionId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
