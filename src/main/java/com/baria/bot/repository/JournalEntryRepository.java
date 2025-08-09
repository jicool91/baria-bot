package com.baria.bot.repository;

import com.baria.bot.model.JournalEntry;
import com.baria.bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByUser(User user);
}
