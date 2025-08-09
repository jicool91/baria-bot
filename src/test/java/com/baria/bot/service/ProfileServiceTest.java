package com.baria.bot.service;

import com.baria.bot.model.User;
import com.baria.bot.repository.JournalEntryRepository;
import com.baria.bot.repository.ReminderRepository;
import com.baria.bot.repository.SymptomLogRepository;
import com.baria.bot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class ProfileServiceTest {
    @Test
    public void testDeleteUser() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        ReminderRepository reminderRepository = Mockito.mock(ReminderRepository.class);
        SymptomLogRepository symptomLogRepository = Mockito.mock(SymptomLogRepository.class);
        JournalEntryRepository journalEntryRepository = Mockito.mock(JournalEntryRepository.class);
        ProfileService service = new ProfileService(userRepository, reminderRepository, symptomLogRepository, journalEntryRepository);
        User u = new User();
        u.setId(1L);
        Mockito.when(userRepository.findByTgId(1L)).thenReturn(Optional.of(u));
        service.deleteUser(1L);
        Mockito.verify(reminderRepository).deleteAllByUser(u);
        Mockito.verify(symptomLogRepository).deleteAllByUser(u);
        Mockito.verify(journalEntryRepository).deleteAllByUser(u);
        Mockito.verify(userRepository).delete(u);
    }
}
