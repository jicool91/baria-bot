package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "symptom_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String symptom;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SymptomStatus status = SymptomStatus.ACTIVE;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum SymptomStatus {
        ACTIVE("active"),
        RESOLVED("resolved");
        
        private final String value;
        
        SymptomStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
