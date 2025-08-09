package com.baria.bot.service;

import com.baria.bot.model.Reminder;
import com.baria.bot.repository.ReminderRepository;
import com.baria.bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Reminder createReminder(Reminder reminder) {
        return reminderRepository.save(reminder);
    }

    public List<Reminder> getUserReminders(Long userId) {
        return userRepository.findById(userId)
                .map(reminderRepository::findByUserAndEnabledTrue)
                .orElse(List.of());
    }

    @Transactional
    public void deleteByUser(Long userId) {
        userRepository.findById(userId).ifPresent(reminderRepository::deleteAllByUser);
    }

    @Transactional
    public void disableReminder(Long id) {
        reminderRepository.findById(id).ifPresent(r -> {
            r.setEnabled(false);
            reminderRepository.save(r);
        });
    }
}
