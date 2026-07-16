package com.site.toffCo.module.odoo.entity;

import com.site.toffCo.module.odoo.dto.OdooEventStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@ToString
@Table(
        name = "odoo_processed_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_odoo_processed_move_line",
                        columnNames = "odoo_move_line_id"
                )
        }
)
@NoArgsConstructor
public class ProcessedOdooEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "odoo_move_line_id",unique = true, nullable = false)
    private Long odooMoveLineId;
    private String productBarcode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OdooEventStatus status; //Sucess or Failed

    private String errorMessage;
    private LocalDateTime processedAt;
}
