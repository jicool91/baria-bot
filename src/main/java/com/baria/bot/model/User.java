package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tg_id", nullable = false, unique = true)
    private Long tgId;

    private String username;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(name = "surgery_date")
    private LocalDate surgeryDate;

    private String role = "patient";

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "current_phase")
    private String currentPhase;

    @Column(name = "dietary_restrictions")
    private String dietaryRestrictions;

    @Column(name = "doctor_code")
    private String doctorCode;

    @Column(name = "consent_given")
    private Boolean consentGiven = false;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
