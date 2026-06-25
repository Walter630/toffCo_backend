package com.site.toffCo.module.whatzap.entity;

import com.site.toffCo.module.whatzap.dto.ChatState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_whatsapp")
public class Whatzap {

    @Id
    private String whatsappId;
    @Enumerated(EnumType.STRING)
    private ChatState currentState;
    private int currentPage;
    private boolean humanAssigned;

    private LocalDateTime lastInteractionAt;
}
