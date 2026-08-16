package com.aicustomer.repository;

import com.aicustomer.entity.Goods;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Goods 实体仓储层。
 */
@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {

    /** 按商户 ID 分页查询商品列表 */
    Page<Goods> findByCtId(Long ctId, Pageable pageable);
}
