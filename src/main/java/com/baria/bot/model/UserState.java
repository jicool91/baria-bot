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
@Table(name = "user_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserState {
    @Id
    private Long userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FsmState state;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum FsmState {
        START,
        MAIN_MENU,
        // Каталог
        CATALOG_BROWSE,
        CATALOG_CATEGORY,
        CATALOG_ITEM,
        CATALOG_SEARCH,
        // Онбординг
        ONBOARDING_CONSENT,
        ONBOARDING_DATE,
        ONBOARDING_MEASURES,
        ONBOARDING_PHASE,
        ONBOARDING_RESTRICTIONS,
        ONBOARDING_SYMPTOMS,
        ONBOARDING_TZ,
        ONBOARDING_SUPPS,
        ONBOARDING_SUMMARY,
        // Симптомы
        SYMPTOM_TRIAGE,
        URGENT_ALERT,
        SYMPTOM_SELFCARE,
        // Напоминания
        REMINDERS_SETUP,
        REMINDERS_ACTIVE,
        // План питания
        PLAN_GENERATE,
        PLAN_VIEW,
        // Профиль
        PROFILE_VIEW,
        PROFILE_EDIT
    }
}
