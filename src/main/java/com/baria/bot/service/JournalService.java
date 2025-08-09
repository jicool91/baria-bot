package com.baria.bot.service;

import com.baria.bot.model.JournalEntry;
import com.baria.bot.model.User;
import com.baria.bot.repository.JournalEntryRepository;
import com.baria.bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalService {
    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;

    public JournalService(JournalEntryRepository journalEntryRepository, UserRepository userRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addEntry(Long tgId, Double weightKg, Integer mood, String symptoms) {
        User user = userRepository.findByTgId(tgId).orElseThrow();
        JournalEntry entry = new JournalEntry();
        entry.setUser(user);
        entry.setWeightKg(weightKg);
        entry.setMood(mood);
        entry.setSymptoms(symptoms);
        journalEntryRepository.save(entry);
    }
}

