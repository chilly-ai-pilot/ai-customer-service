package com.aicustomer.repository;

import com.aicustomer.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Session 实体仓储层，提供会话的持久化和查询操作。
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * 按用户+商户+商品查找唯一会话（用于首条消息时判断是否已存在）。
     */
    Optional<Session> findByUserIdAndCtIdAndGoodsId(Long userId, Long ctId, Long goodsId);

    /**
     * 商户查询自己的全部会话，按最后消息时间倒序。
     */
    Page<Session> findByCtIdOrderByLastMessageTimeDesc(Long ctId, Pageable pageable);

    /**
     * 用户查询自己的全部会话，按最后消息时间倒序。
     */
    Page<Session> findByUserIdOrderByLastMessageTimeDesc(Long userId, Pageable pageable);

    /**
     * 按 sessionId 和参与者 ID（userId 或 ctId 二选一）查找会话。
     * 用于会话归属校验，防止越权访问。
     */
    @Query("SELECT s FROM Session s WHERE s.id = :sessionId AND (s.userId = :userId OR s.ctId = :ctId)")
    Optional<Session> findByIdAndParticipant(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("ctId") Long ctId
    );
}
