package com.site.toffCo.module.odoo.repository;

import com.site.toffCo.module.odoo.dto.OdooEventStatus;
import com.site.toffCo.module.odoo.entity.ProcessedOdooEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedOdooEventRepository extends JpaRepository<ProcessedOdooEvent, UUID> {
    boolean existsByOdooMoveLineId(Long odooMoveLineId);
    long countByStatus(OdooEventStatus status);
    Page<ProcessedOdooEvent> findByStatus(OdooEventStatus status, Pageable pageable);
}
