package com.baria.bot.handler;

import com.baria.bot.model.User;
import com.baria.bot.model.User.Phase;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PlanHandlerTest {
    @Test
    public void testGeneratePlan() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AiService aiService = Mockito.mock(AiService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        User u = new User();
        u.setId(1L);
        u.setPhase(Phase.LIQUID);
        u.setRestrictions(java.util.List.of());
        Mockito.when(userRepository.findByTgId(1L)).thenReturn(Optional.of(u));
        PlanHandler handler = new PlanHandler(userRepository, aiService, objectMapper);
        SendMessage msg = handler.generatePlan(1L);
        assertNotNull(msg.getText());
    }
}
