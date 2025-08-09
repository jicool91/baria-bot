package com.baria.bot.repository;

import com.baria.bot.model.Reminder;
import com.baria.bot.model.Reminder.ReminderType;
import com.baria.bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    List<Reminder> findByUserAndEnabledTrue(User user);
    
    List<Reminder> findByUserAndType(User user, ReminderType type);
    
    List<Reminder> findByEnabledTrue();
    
    void deleteAllByUser(User user);
}
