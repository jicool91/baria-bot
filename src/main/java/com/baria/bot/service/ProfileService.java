package com.baria.bot.service;

import com.baria.bot.model.User;
import com.baria.bot.model.User.Goal;
import com.baria.bot.model.User.Phase;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.repository.ReminderRepository;
import com.baria.bot.repository.SymptomLogRepository;
import com.baria.bot.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final SymptomLogRepository symptomLogRepository;
    private final JournalEntryRepository journalEntryRepository;

    public ProfileService(UserRepository userRepository, ReminderRepository reminderRepository,
                          SymptomLogRepository symptomLogRepository, JournalEntryRepository journalEntryRepository) {
        this.userRepository = userRepository;
        this.reminderRepository = reminderRepository;
        this.symptomLogRepository = symptomLogRepository;
        this.journalEntryRepository = journalEntryRepository;
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
    public void updateRestrictions(Long tgId, java.util.List<String> restrictions) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setRestrictions(restrictions);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updatePhysical(Long tgId, Integer heightCm, java.math.BigDecimal weightKg) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setHeightCm(heightCm);
            u.setWeightKg(weightKg);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updatePhase(Long tgId, Phase phase) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setPhase(phase);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updateGoal(Long tgId, Goal goal) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            u.setGoal(goal);
            userRepository.save(u);
        });
    }

    @Transactional
    public void deleteUser(Long tgId) {
        userRepository.findByTgId(tgId).ifPresent(u -> {
            reminderRepository.deleteAllByUser(u);
            symptomLogRepository.deleteAllByUser(u);
            journalEntryRepository.deleteAllByUser(u);
            userRepository.delete(u);
        });
    }
}

