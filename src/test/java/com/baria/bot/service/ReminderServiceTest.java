package com.baria.bot.service;

import com.baria.bot.model.Reminder;
import com.baria.bot.model.User;
import com.baria.bot.repository.ReminderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;


public class ReminderServiceTest {
    @Test
    public void testScheduling() throws Exception {
        ReminderRepository reminderRepository = Mockito.mock(ReminderRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        ReminderService service = new ReminderService(reminderRepository, userRepository, scheduler);
        User user = new User();
        user.setId(1L);
        user.setTimezone("Europe/Moscow");
        Mockito.when(reminderRepository.save(Mockito.any())).thenAnswer(i -> {
            Reminder r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        Reminder reminder = Reminder.builder()
                .user(user)
                .rrule("FREQ=DAILY;BYHOUR=9,10")
                .build();
        service.createReminder(reminder);
        Mockito.verify(scheduler, Mockito.times(2)).scheduleJob(Mockito.any(), Mockito.any());
    }
}
