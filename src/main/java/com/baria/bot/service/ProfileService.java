package com.baria.bot.service;

import com.baria.bot.model.User;
import com.baria.bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ProfileService {
    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getByTgId(Long tgId) {
        return userRepository.findByTgId(tgId);
    }

    @Transactional
    public void updateBasicInfo(Long tgId, String fullName, LocalDate surgeryDate) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setFullName(fullName);
            u.setSurgeryDate(surgeryDate);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updateRestrictions(Long tgId, String restrictions) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setDietaryRestrictions(restrictions);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updatePhysical(Long tgId, Integer heightCm, Double weightKg) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setHeightCm(heightCm);
            u.setWeightKg(weightKg);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updatePhase(Long tgId, String phase) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setCurrentPhase(phase);
            userRepository.save(u);
        });
    }
}

