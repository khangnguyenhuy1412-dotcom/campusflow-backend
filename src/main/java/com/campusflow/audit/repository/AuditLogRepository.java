package com.campusflow.audit.repository;

import com.campusflow.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Override
    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findAll(Pageable pageable);
}
