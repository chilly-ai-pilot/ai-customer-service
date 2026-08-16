package com.aicustomer.repository;

import com.aicustomer.entity.CommercialTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CommercialTenant 实体仓储层。
 */
@Repository
public interface CommercialTenantRepository extends JpaRepository<CommercialTenant, Long> {

    /** 按账号精确查找商户 */
    Optional<CommercialTenant> findByAccount(String account);
}
