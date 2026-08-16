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

/**
 * Message 实体仓储层，提供消息的持久化和查询操作。
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 按会话 ID 查询消息（分页），按时间正序 */
    Page<Message> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    /** 按会话 ID 查询消息（分页），按时间倒序 */
    Page<Message> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    /** 统计某接收者的全部未读消息数 */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId " +
           "AND m.receiverType = :receiverType AND m.isRead = false")
    long countUnreadByReceiver(@Param("receiverId") Long receiverId,
                              @Param("receiverType") SubjectType receiverType);

    /** 统计某会话中某接收者的未读消息数 */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.sessionId = :sessionId " +
           "AND m.receiverId = :receiverId AND m.receiverType = :receiverType AND m.isRead = false")
    long countUnreadBySessionAndReceiver(@Param("sessionId") Long sessionId,
                                         @Param("receiverId") Long receiverId,
                                         @Param("receiverType") SubjectType receiverType);

    /** 批量标记某会话中某接收者的未读消息为已读，返回受影响行数 */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt " +
           "WHERE m.sessionId = :sessionId AND m.receiverId = :receiverId " +
           "AND m.receiverType = :receiverType AND m.isRead = false")
    int markReadBySessionAndReceiver(@Param("sessionId") Long sessionId,
                                     @Param("receiverId") Long receiverId,
                                     @Param("receiverType") SubjectType receiverType,
                                     @Param("readAt") Instant readAt);

    /** 批量按 messageId 查找消息（用于幂等校验） */
    List<Message> findByMessageIdIn(List<String> messageIds);
}
