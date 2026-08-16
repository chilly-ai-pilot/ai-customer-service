package com.aicustomer.repository;

import com.aicustomer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 实体仓储层。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按账号精确查找用户 */
    Optional<User> findByAccount(String account);
}
