package com.baria.bot.handler;

import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class OnboardingHandlerTest {
    @Test
    public void testFlow() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        OnboardingService service = new OnboardingService(userRepository);
        OnboardingHandler handler = new OnboardingHandler(service, userRepository);
        String start = handler.start(1L, "user");
        assertTrue(start.contains("согласен"));
        handler.handle(1L, "согласен");
        String next = handler.handle(1L, "01.01.2024");
        assertTrue(next.contains("рост"));
    }
}
