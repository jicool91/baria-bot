package com.baria.bot.service;

import com.baria.bot.config.BotProperties;
import com.baria.bot.handler.AiHandler;
import com.baria.bot.handler.JournalHandler;
import com.baria.bot.handler.OnboardingHandler;
import com.baria.bot.handler.ProfileHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class BariaBot extends TelegramLongPollingBot {
    private final BotProperties properties;
    private final OnboardingHandler onboardingHandler;
    private final ProfileHandler profileHandler;
    private final JournalHandler journalHandler;
    private final AiHandler aiHandler;

    public BariaBot(BotProperties properties,
                    OnboardingHandler onboardingHandler,
                    ProfileHandler profileHandler,
                    JournalHandler journalHandler,
                    AiHandler aiHandler) {
        super(properties.getToken());
        this.properties = properties;
        this.onboardingHandler = onboardingHandler;
        this.profileHandler = profileHandler;
        this.journalHandler = journalHandler;
        this.aiHandler = aiHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                sendMessage(chatId, onboardingHandler.start(chatId, update.getMessage().getFrom().getUserName()));
                return;
            }

            if (onboardingHandler.isOnboarding(chatId)) {
                sendMessage(chatId, onboardingHandler.handle(chatId, text));
                return;
            }

            if (profileHandler.isEditingRestrictions(chatId)) {
                sendMessage(chatId, profileHandler.saveRestrictions(chatId, text));
                return;
            }

            if (journalHandler.isAwaiting(chatId)) {
                sendMessage(chatId, journalHandler.save(chatId, text));
                return;
            }

            switch (text) {
                case "/profile", "\uD83D\uDC64 Профиль" -> sendMessage(chatId, profileHandler.showProfile(chatId));
                case "\uD83D\uDEAB Ограничения", "\uD83D\uDEAB", "Ограничения" -> sendMessage(chatId, profileHandler.startEditing(chatId));
                case "/journal" -> sendMessage(chatId, journalHandler.start(chatId));
                default -> sendMessage(chatId, aiHandler.ask(text));
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }
}
