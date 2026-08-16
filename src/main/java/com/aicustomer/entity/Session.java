package com.aicustomer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ct_id", nullable = false)
    private Long ctId;

    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_status", nullable = false, length = 16)
    private ConversationStatus conversationStatus;

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

    public enum ConversationStatus {
        ACTIVE,
        CLOSED
    }
}
