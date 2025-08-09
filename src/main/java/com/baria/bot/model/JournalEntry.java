package com.baria.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "journals")
@Getter
@Setter
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User user;

    private LocalDate entryDate = LocalDate.now();
    private Double weightKg;
    private Integer mood;
    private String symptoms;
}

