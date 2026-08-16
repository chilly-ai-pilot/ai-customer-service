package com.aicustomer.service;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.dto.response.SessionResponse;
import com.aicustomer.entity.Message;
import com.aicustomer.entity.Session;
import com.aicustomer.repository.MessageRepository;
import com.aicustomer.repository.SessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 会话服务，提供会话列表查询和响应体转换。
 */
@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public SessionService(SessionRepository sessionRepository, MessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 商户查询自己的会话列表。
     *
     * @param ctId     商户 ID（从 token 解析）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页会话列表
     */
    public Page<SessionResponse> listByTenant(Long ctId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        Page<Session> page = sessionRepository.findByCtIdOrderByLastMessageTimeDesc(ctId, pageRequest);
        return page.map(session -> toDetailResponse(session, ctId, SubjectType.TENANT));
    }

    /**
     * 用户查询自己的会话列表。
     *
     * @param userId   用户 ID（从 token 解析）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页会话列表
     */
    public Page<SessionResponse> listByUser(Long userId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        Page<Session> page = sessionRepository.findByUserIdOrderByLastMessageTimeDesc(userId, pageRequest);
        return page.map(session -> toDetailResponse(session, userId, SubjectType.USER));
    }

    /**
     * 将 Session 实体转为不含 lastMessageContent 和 unreadCount 的基础响应（用于会话详情接口）。
     */
    public SessionResponse toResponse(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .ctId(session.getCtId())
                .goodsId(session.getGoodsId())
                .conversationStatus(session.getConversationStatus())
                .lastMessageTime(session.getLastMessageTime())
                .createdAt(session.getCreatedAt())
                .build();
    }

    /**
     * 将 Session 实体转为含 lastMessageContent 和 unreadCount 的完整响应（用于列表接口）。
     */
    private SessionResponse toDetailResponse(Session session, Long viewerId, SubjectType viewerType) {
        // 查询最后一条消息内容（截断至 50 字）
        PageRequest lastMsgRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> lastMsgPage = messageRepository.findBySessionIdOrderByCreatedAtDesc(
                session.getId(), lastMsgRequest);
        String lastContent = null;
        if (lastMsgPage.hasContent()) {
            String content = lastMsgPage.getContent().get(0).getContent();
            lastContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }

        // 统计当前用户在该会话中的未读数
        long unreadCount = messageRepository.countUnreadBySessionAndReceiver(
                session.getId(), viewerId, viewerType);

        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .ctId(session.getCtId())
                .goodsId(session.getGoodsId())
                .conversationStatus(session.getConversationStatus())
                .lastMessageTime(session.getLastMessageTime())
                .createdAt(session.getCreatedAt())
                .lastMessageContent(lastContent)
                .unreadCount(unreadCount)
                .build();
    }
}
