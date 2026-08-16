package com.aicustomer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品实体，由商户创建并管理。
 */
@Entity
@Table(name = "goods_t")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    /** 所属商户 ID */
    @Column(nullable = false)
    private Long ctId;
}
