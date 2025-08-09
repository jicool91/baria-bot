package com.baria.bot.handler;

import com.baria.bot.service.JournalService;
import com.baria.bot.service.RedFlagService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class JournalHandler {
    private final JournalService journalService;
    private final RedFlagService redFlagService;
    private final Set<Long> waitingForEntry = new HashSet<>();

    public JournalHandler(JournalService journalService, RedFlagService redFlagService) {
        this.journalService = journalService;
        this.redFlagService = redFlagService;
    }

    public boolean isAwaiting(Long chatId) {
        return waitingForEntry.contains(chatId);
    }

    public String start(Long chatId) {
        waitingForEntry.add(chatId);
        return "Введите через запятую: вес, настроение (1-5), симптомы";
    }

    public String save(Long chatId, String text) {
        waitingForEntry.remove(chatId);
        String[] parts = text.split(",");
        if (parts.length < 3) {
            return "Неверный формат";
        }
        try {
            Double weight = Double.parseDouble(parts[0].trim());
            Integer mood = Integer.parseInt(parts[1].trim());
            String symptoms = parts[2].trim();
            journalService.addEntry(chatId, weight, mood, symptoms);
            if (symptoms.toLowerCase().contains("боль")) {
                redFlagService.registerFlag(chatId, symptoms, 2);
            }
            return "Запись сохранена";
        } catch (Exception e) {
            return "Ошибка сохранения";
        }
    }
}

