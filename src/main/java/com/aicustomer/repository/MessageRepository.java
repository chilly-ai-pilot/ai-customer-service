package com.aicustomer.repository;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    Page<Message> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId " +
           "AND m.receiverType = :receiverType AND m.isRead = false")
    long countUnreadByReceiver(@Param("receiverId") Long receiverId,
                              @Param("receiverType") SubjectType receiverType);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sessionId = :sessionId " +
           "AND m.receiverId = :receiverId AND m.receiverType = :receiverType AND m.isRead = false")
    long countUnreadBySessionAndReceiver(@Param("sessionId") Long sessionId,
                                         @Param("receiverId") Long receiverId,
                                         @Param("receiverType") SubjectType receiverType);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt " +
           "WHERE m.sessionId = :sessionId AND m.receiverId = :receiverId " +
           "AND m.receiverType = :receiverType AND m.isRead = false")
    int markReadBySessionAndReceiver(@Param("sessionId") Long sessionId,
                                     @Param("receiverId") Long receiverId,
                                     @Param("receiverType") SubjectType receiverType,
                                     @Param("readAt") Instant readAt);

    List<Message> findByMessageIdIn(List<String> messageIds);
}
