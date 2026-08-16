package com.aicustomer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体（普通消费者）。
 */
@Entity
@Table(name = "user_t")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String account;

    /** 密码存储为 SHA-256 哈希值 */
    @Column(nullable = false, length = 64)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;
}
