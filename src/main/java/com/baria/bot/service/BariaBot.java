package com.baria.bot.service;

import com.baria.bot.config.BotProperties;
import com.baria.bot.handler.AiHandler;
import com.baria.bot.handler.JournalHandler;
import com.baria.bot.handler.OnboardingHandler;
import com.baria.bot.handler.ProfileHandler;
import com.baria.bot.util.KeyboardUtils;
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
                sendMessage(chatId, onboardingHandler.start(chatId, update.getMessage().getFrom().getUserName()),
                        KeyboardUtils.removeKeyboard());
                return;
            }

            if (onboardingHandler.isOnboarding(chatId)) {
                String response = onboardingHandler.handle(chatId, text);
                if (onboardingHandler.isOnboarding(chatId)) {
                    sendMessage(chatId, response, KeyboardUtils.removeKeyboard());
                } else {
                    sendMessage(chatId, response, KeyboardUtils.mainMenu());
                }
                return;
            }

            if (profileHandler.isEditingRestrictions(chatId)) {
                String resp = profileHandler.saveRestrictions(chatId, text);
                sendMessage(chatId, resp, KeyboardUtils.mainMenu());
                return;
            }

            if (journalHandler.isAwaiting(chatId)) {
                String resp = journalHandler.save(chatId, text);
                sendMessage(chatId, resp, KeyboardUtils.mainMenu());
                return;
            }

            switch (text) {
                case "/profile", "\uD83D\uDC64 Профиль" ->
                        sendMessage(chatId, profileHandler.showProfile(chatId), KeyboardUtils.mainMenu());
                case "\uD83D\uDEAB Ограничения", "\uD83D\uDEAB", "Ограничения" ->
                        sendMessage(chatId, profileHandler.startEditing(chatId), KeyboardUtils.removeKeyboard());
                case "/journal", "\uD83D\uDCDD Журнал" ->
                        sendMessage(chatId, journalHandler.start(chatId), KeyboardUtils.removeKeyboard());
                default -> sendMessage(chatId, aiHandler.ask(text), KeyboardUtils.mainMenu());
            }
        }
    }

    private void sendMessage(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder().chatId(chatId.toString()).text(text);
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }
            execute(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }
    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }
}
