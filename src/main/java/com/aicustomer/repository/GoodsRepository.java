package com.aicustomer.repository;

import com.aicustomer.entity.Goods;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {

    Page<Goods> findByCtId(Long ctId, Pageable pageable);
}
