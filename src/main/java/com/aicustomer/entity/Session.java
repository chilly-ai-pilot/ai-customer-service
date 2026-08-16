package com.aicustomer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话实体，关联一个用户与一个商户围绕一个商品的一次咨询会话。
 * 同一用户对同一商户同一商品只维护一条会话（按 uk_user_ct_goods 唯一约束保证）。
 */
@Entity
@Table(name = "session_t",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_ct_goods",
                columnNames = {"user_id", "ct_id", "goods_id"}
        ),
        indexes = {
                @Index(name = "idx_user_ct_goods", columnList = "user_id, ct_id, goods_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_ct_id", columnList = "ct_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发起会话的用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 会话所属的商户 ID */
    @Column(name = "ct_id", nullable = false)
    private Long ctId;

    /** 咨询的商品 ID */
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    /** 会话状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_status", nullable = false, length = 16)
    private ConversationStatus conversationStatus;

    /** 最后一条消息的时间（用于收件箱列表排序） */
    @Column(name = "last_message_time", nullable = false)
    private Instant lastMessageTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (conversationStatus == null) {
            conversationStatus = ConversationStatus.ACTIVE;
        }
        if (lastMessageTime == null) {
            lastMessageTime = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /** 会话状态枚举 */
    public enum ConversationStatus {
        /** 会话进行中 */
        ACTIVE,
        /** 会话已关闭 */
        CLOSED
    }
}
