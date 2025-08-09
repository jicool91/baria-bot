package com.baria.bot.handler;

import com.baria.bot.service.AiService;
import org.springframework.stereotype.Component;

@Component
public class AiHandler {
    private final AiService aiService;

    public AiHandler(AiService aiService) {
        this.aiService = aiService;
    }

    public String ask(String question) {
        return aiService.ask(question);
    }
}

