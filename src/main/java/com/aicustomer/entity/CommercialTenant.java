package com.aicustomer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户（商业主体）实体。
 */
@Entity
@Table(name = "commercial_tenant_t")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialTenant {

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
