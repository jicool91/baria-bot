package com.baria.bot.service;

import com.baria.bot.model.User;
import com.baria.bot.repository.DoctorCodeRepository;
import com.baria.bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class OnboardingService {
    private final UserRepository userRepository;
    private final DoctorCodeRepository doctorCodeRepository;

    public OnboardingService(UserRepository userRepository, DoctorCodeRepository doctorCodeRepository) {
        this.userRepository = userRepository;
        this.doctorCodeRepository = doctorCodeRepository;
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
    public void saveBasicInfo(Long tgId, String fullName, LocalDate surgeryDate) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setFullName(fullName);
            u.setSurgeryDate(surgeryDate);
            userRepository.save(u);
        });
    }

    @Transactional
    public Optional<String> saveDoctorCode(Long tgId, String code) {
        return doctorCodeRepository.findById(code).map(dc -> {
            userRepository.findByTgId(tgId).ifPresent(u -> {
                u.setDoctorCode(code);
                userRepository.save(u);
            });
            return dc.getDoctorName();
        });
    }

    @Transactional
    public void saveConsent(Long tgId) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setConsentGiven(true);
            u.setOnboardingCompleted(true);
            userRepository.save(u);
        });
    }
}

