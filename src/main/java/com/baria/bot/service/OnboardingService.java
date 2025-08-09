package com.baria.bot.service;

import com.baria.bot.model.User;
import com.baria.bot.model.User.Goal;
import com.baria.bot.model.User.Phase;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.repository.SymptomLogRepository;
import com.baria.bot.model.SymptomLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class OnboardingService {
    private final UserRepository userRepository;
    private final SymptomLogRepository symptomLogRepository;

    public OnboardingService(UserRepository userRepository, SymptomLogRepository symptomLogRepository) {
        this.userRepository = userRepository;
        this.symptomLogRepository = symptomLogRepository;
    }

    public User getOrCreate(Long tgId, String username) {
        return userRepository.findByTgId(tgId).orElseGet(() -> {
            User u = new User();
            u.setTgId(tgId);
            u.setUsername(username);
            return userRepository.save(u);
        });
    }

    @Transactional
    public void saveSurgeryDate(Long tgId, LocalDate surgeryDate) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setSurgeryDate(surgeryDate);
            userRepository.save(u);
        });
    }

    @Transactional
    public void saveConsent(Long tgId) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setConsentAt(java.time.LocalDateTime.now());
            userRepository.save(u);
        });
    }

    @Transactional
    public void saveMeasures(Long tgId, Integer height, java.math.BigDecimal weight, Goal goal) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setHeightCm(height);
            u.setWeightKg(weight);
            u.setGoal(goal);
            userRepository.save(u);
        });
    }

    @Transactional
    public void savePhase(Long tgId, Phase phase, User.PhaseMode mode) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setPhase(phase);
            u.setPhaseMode(mode);
            userRepository.save(u);
        });
    }

    @Transactional
    public void saveRestrictions(Long tgId, java.util.List<String> restrictions) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setRestrictions(restrictions);
            userRepository.save(u);
        });
    }

    @Transactional
    public void saveSymptoms(Long tgId, java.util.List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) return;
        userRepository.findByTgId(tgId).ifPresent(u -> {
            for (String s : symptoms) {
                SymptomLog log = new SymptomLog();
                log.setUser(u);
                log.setSymptom(s);
                log.setStartedAt(java.time.LocalDateTime.now());
                log.setStatus("active");
                symptomLogRepository.save(log);
            }
        });
    }

    @Transactional
    public void saveTimezone(Long tgId, String tz) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setTimezone(tz);
            userRepository.save(u);
        });
    }

    @Transactional
    public void saveSupplements(Long tgId, java.util.List<String> supps) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setSupplements(supps);
            userRepository.save(u);
        });
    }
}

