package com.aicustomer.repository;

import com.aicustomer.entity.CommercialTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommercialTenantRepository extends JpaRepository<CommercialTenant, Long> {
    
    Optional<CommercialTenant> findByAccount(String account);
}
