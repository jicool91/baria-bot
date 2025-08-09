package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReminderType type;
    
    @Column(nullable = false)
    private String rrule; // iCal RRULE format
    
    @Column(name = "local_time")
    private String localTime; // HH:MM format
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum ReminderType {
        WATER("water", "💧 Вода"),
        MEAL("meal", "🍽 Прием пищи"),
        SUPPLEMENT("supplement", "💊 Витамины"),
        SYMPTOM_WATCH("symptom_watch", "⚠️ Мониторинг симптома");
        
        private final String code;
        private final String displayName;
        
        ReminderType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
