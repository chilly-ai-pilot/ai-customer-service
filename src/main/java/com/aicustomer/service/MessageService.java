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
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Page<MessageResponse> listMessages(Session session, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> page = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId(), pageRequest);
        return page.map(this::toResponse);
    }

    @Transactional
    public Map<String, Object> markRead(Session session, CurrentUser currentUser) {
        Instant readAt = Instant.now();
        int count = messageRepository.markReadBySessionAndReceiver(
                session.getId(), currentUser.getId(), currentUser.getType(), readAt);
        return Map.of(
                "sessionId", session.getId(),
                "markedCount", count,
                "readAt", readAt
        );
    }

    public long countUnread(Session session, CurrentUser currentUser) {
        return messageRepository.countUnreadBySessionAndReceiver(
                session.getId(), currentUser.getId(), currentUser.getType());
    }

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
