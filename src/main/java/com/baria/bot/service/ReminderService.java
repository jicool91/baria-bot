package com.baria.bot.service;

import com.baria.bot.model.Reminder;
import com.baria.bot.repository.ReminderRepository;
import com.baria.bot.repository.UserRepository;
import org.quartz.*;
import java.util.TimeZone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final Scheduler scheduler;

    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository, Scheduler scheduler) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.scheduler = scheduler;
    }

    @Transactional
    public Reminder createReminder(Reminder reminder) {
        Reminder saved = reminderRepository.save(reminder);
        scheduleReminder(saved);
        return saved;
    }

    public List<Reminder> getUserReminders(Long userId) {
        return userRepository.findById(userId)
                .map(reminderRepository::findByUserAndEnabledTrue)
                .orElse(List.of());
    }

    @Transactional
    public void deleteByUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            reminderRepository.deleteAllByUser(user);
        });
    }

    @Transactional
    public void disableReminder(Long id) {
        reminderRepository.findById(id).ifPresent(r -> {
            r.setEnabled(false);
            reminderRepository.save(r);
        });
    }

    private void scheduleReminder(Reminder reminder) {
        try {
            String rrule = reminder.getRrule();
            if (rrule == null) return;
            String byHourPart = null;
            for (String part : rrule.split(";")) {
                if (part.startsWith("BYHOUR=")) {
                    byHourPart = part.substring("BYHOUR=".length());
                }
            }
            if (byHourPart == null) return;
            String tz = reminder.getUser().getTimezone() != null ? reminder.getUser().getTimezone() : "UTC";
            for (String hourStr : byHourPart.split(",")) {
                int hour = Integer.parseInt(hourStr);
                JobDetail job = JobBuilder.newJob(ReminderJob.class)
                        .withIdentity("reminder-" + reminder.getId() + "-" + hour)
                        .build();
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(hour, 0)
                                .inTimeZone(TimeZone.getTimeZone(tz)))
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception e) {
            // ignore scheduling errors for now
        }
    }
}
