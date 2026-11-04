package com.nexus.nkap.repository;

import com.nexus.nkap.domain.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 租户数据访问层（tenant 表）。
 */
@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * 按租户编码查询。
     */
    Optional<TenantEntity> findByCode(String code);

    /**
     * 判断指定租户编码是否已存在（唯一约束 uk_tenant_code 前置校验）。
     */
    boolean existsByCode(String code);
}
