package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tg_id", unique = true, nullable = false)
    private Long tgId;
    
    private String username;
    
    @Column(name = "full_name")
    private String fullName;
    
    private String phone;
    
    @Column(name = "surgery_date")
    private LocalDate surgeryDate;
    
    @Column(name = "consent_at")
    private LocalDateTime consentAt;
    
    @Column(name = "tz")
    private String timezone = "Europe/Moscow";
    
    @Enumerated(EnumType.STRING)
    private Phase phase;
    
    @Column(name = "phase_mode")
    @Enumerated(EnumType.STRING)
    private PhaseMode phaseMode = PhaseMode.AUTO;
    
    @Column(name = "height_cm")
    private Integer heightCm;
    
    @Column(name = "weight_kg")
    private BigDecimal weightKg;
    
    @Enumerated(EnumType.STRING)
    private Goal goal;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> restrictions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> supplements;
    
    @Column(nullable = false)
    private String role = "patient";
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Вычисляемое поле для онбординга
    @Transient
    public boolean isOnboardingCompleted() {
        return consentAt != null && surgeryDate != null && heightCm != null && weightKg != null;
    }
    
    // Автоматическое определение фазы на основе даты операции
    @Transient
    public Phase calculateAutoPhase() {
        if (surgeryDate == null) return null;
        
        long daysSinceSurgery = java.time.temporal.ChronoUnit.DAYS.between(surgeryDate, LocalDate.now());
        
        if (daysSinceSurgery <= 14) {
            return Phase.LIQUID;
        } else if (daysSinceSurgery <= 28) {
            return Phase.PUREED;
        } else if (daysSinceSurgery <= 60) {
            return Phase.SOFT;
        } else {
            return Phase.REGULAR;
        }
    }
    
    public enum Phase {
        LIQUID("liquid", "Жидкая"),
        PUREED("pureed", "Пюре"),
        SOFT("soft", "Мягкая"),
        REGULAR("regular", "Обычная");
        
        private final String code;
        private final String displayName;
        
        Phase(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static Phase fromCode(String code) {
            for (Phase phase : values()) {
                if (phase.code.equalsIgnoreCase(code)) {
                    return phase;
                }
            }
            return null;
        }
    }
    
    public enum PhaseMode {
        AUTO("auto"),
        MANUAL("manual");
        
        private final String value;
        
        PhaseMode(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum Goal {
        LOSE("lose", "Снижение веса"),
        MAINTAIN("maintain", "Удержание веса"),
        PROTEIN("protein", "Набор белка");
        
        private final String code;
        private final String displayName;
        
        Goal(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static Goal fromCode(String code) {
            for (Goal goal : values()) {
                if (goal.code.equalsIgnoreCase(code)) {
                    return goal;
                }
            }
            return null;
        }
    }
}
