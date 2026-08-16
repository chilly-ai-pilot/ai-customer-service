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

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public SessionService(SessionRepository sessionRepository, MessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public Page<SessionResponse> listByTenant(Long ctId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        Page<Session> page = sessionRepository.findByCtIdOrderByLastMessageTimeDesc(ctId, pageRequest);
        return page.map(session -> toResponse(session, ctId, SubjectType.TENANT));
    }

    public Page<SessionResponse> listByUser(Long userId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        Page<Session> page = sessionRepository.findByUserIdOrderByLastMessageTimeDesc(userId, pageRequest);
        return page.map(session -> toResponse(session, userId, SubjectType.USER));
    }

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

    private SessionResponse toResponse(Session session, Long receiverId, SubjectType receiverType) {
        PageRequest lastMsgRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> lastMsgPage = messageRepository.findBySessionIdOrderByCreatedAtDesc(
                session.getId(), lastMsgRequest);
        String lastContent = null;
        if (lastMsgPage.hasContent()) {
            String content = lastMsgPage.getContent().get(0).getContent();
            lastContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }

        long unreadCount = messageRepository.countUnreadBySessionAndReceiver(
                session.getId(), receiverId, receiverType);

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
