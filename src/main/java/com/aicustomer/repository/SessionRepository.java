package com.aicustomer.repository;

import com.aicustomer.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByUserIdAndCtIdAndGoodsId(Long userId, Long ctId, Long goodsId);

    Page<Session> findByCtIdOrderByLastMessageTimeDesc(Long ctId, Pageable pageable);

    Page<Session> findByUserIdOrderByLastMessageTimeDesc(Long userId, Pageable pageable);

    @Query("SELECT s FROM Session s WHERE s.id = :sessionId AND (s.userId = :userId OR s.ctId = :ctId)")
    Optional<Session> findByIdAndParticipant(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("ctId") Long ctId
    );
}
