package com.baria.bot.handler;

import com.baria.bot.model.RedFlag;
import com.baria.bot.repository.RedFlagRepository;
import com.baria.bot.repository.SymptomLogRepository;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.AiService;
import com.baria.bot.service.RedFlagService;
import com.baria.bot.service.ReminderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SymptomsHandlerTest {
    @Test
    public void testRedFlagQuestion() {
        RedFlagService redFlagService = Mockito.mock(RedFlagService.class);
        RedFlagRepository redFlagRepository = Mockito.mock(RedFlagRepository.class);
        SymptomLogRepository symptomLogRepository = Mockito.mock(SymptomLogRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AiService aiService = Mockito.mock(AiService.class);
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        RedFlag rf = new RedFlag();
        rf.setQuestion("Болит ли сильно?");
        Mockito.when(redFlagRepository.findByIsActiveTrue()).thenReturn(List.of(rf));
        SymptomsHandler handler = new SymptomsHandler(redFlagService, redFlagRepository, symptomLogRepository, userRepository, aiService, reminderService);
        SendMessage msg = handler.handleSymptomCheck(1L, "боль");
        assertTrue(msg.getText().contains("Важный вопрос"));
    }
}
