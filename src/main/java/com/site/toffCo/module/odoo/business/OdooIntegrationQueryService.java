package com.site.toffCo.module.odoo.business;

import com.site.toffCo.module.odoo.dto.OdooEventStatus;
import com.site.toffCo.module.odoo.dto.OdooIntegrationSummaryDTO;
import com.site.toffCo.module.odoo.dto.OdooStockResponseDTO;
import com.site.toffCo.module.odoo.entity.ProcessedOdooEvent;
import com.site.toffCo.module.odoo.repository.ProcessedOdooEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OdooIntegrationQueryService {
    private final ProcessedOdooEventRepository processedOdooEventRepository;

    public OdooIntegrationSummaryDTO getOdooSummary() {
        long total = processedOdooEventRepository.count();
        long sucess = processedOdooEventRepository.countByStatus(
                OdooEventStatus.SUCCESS
        );
        long failed = processedOdooEventRepository.countByStatus(
                OdooEventStatus.FAILED
        );

        return new OdooIntegrationSummaryDTO(total, sucess, failed);
    }

    public Page<OdooStockResponseDTO> findAll(
            OdooEventStatus status,
            Pageable pageable
    ) {
        Page<ProcessedOdooEvent> events;

        if (status == null) {
            events = processedOdooEventRepository.findAll(pageable);
        } else {
            events = processedOdooEventRepository.findByStatus(status, pageable);
        }

        return events.map(this::toResponse);
    }

    private OdooStockResponseDTO toResponse(ProcessedOdooEvent event) {
        return new OdooStockResponseDTO(
                event.getId(),
                event.getOdooMoveLineId(),
                event.getProductBarcode(),
                event.getStatus(),
                event.getErrorMessage(),
                event.getProcessedAt()
        );
    }
}
