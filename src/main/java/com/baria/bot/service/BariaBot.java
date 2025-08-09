package com.baria.bot.service;

import com.baria.bot.config.BotProperties;
import com.baria.bot.handler.*;
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
    private final MenuHandler menuHandler;
    private final CatalogHandler catalogHandler;
    private final PlanHandler planHandler;
    private final SymptomsHandler symptomsHandler;
    private final EraseHandler eraseHandler;

    public BariaBot(BotProperties properties,
                    OnboardingHandler onboardingHandler,
                    ProfileHandler profileHandler,
                    JournalHandler journalHandler,
                    AiHandler aiHandler,
                    MenuHandler menuHandler,
                    CatalogHandler catalogHandler,
                    PlanHandler planHandler,
                    SymptomsHandler symptomsHandler,
                    EraseHandler eraseHandler) {
        super(properties.getToken());
        this.properties = properties;
        this.onboardingHandler = onboardingHandler;
        this.profileHandler = profileHandler;
        this.journalHandler = journalHandler;
        this.aiHandler = aiHandler;
        this.menuHandler = menuHandler;
        this.catalogHandler = catalogHandler;
        this.planHandler = planHandler;
        this.symptomsHandler = symptomsHandler;
        this.eraseHandler = eraseHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.equals("/start") || text.equals("/menu")) {
                send(menuHandler.showMainMenu(chatId));
                return;
            }

            if (onboardingHandler.isOnboarding(chatId)) {
                sendText(chatId, onboardingHandler.handle(chatId, text));
                return;
            }

            if (catalogHandler.isAwaitingSearch(chatId)) {
                send(catalogHandler.handleSearch(chatId, text));
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
                case "/profile", "\uD83D\uDC64 Профиль" -> sendText(chatId, profileHandler.showProfile(chatId));
                case "/products" -> send(catalogHandler.handleCatalogBrowse(chatId));
                case "/plan" -> send(planHandler.generatePlan(chatId));
                case "/reminders" -> sendText(chatId, "Напоминания пока не реализованы");
                case "/symptom" -> send(symptomsHandler.handleSymptomCheck(chatId, text));
                case "/erase_me" -> send(eraseHandler.erase(chatId));
                case "\uD83D\uDEAB Ограничения", "\uD83D\uDEAB", "Ограничения" -> sendText(chatId, profileHandler.startEditing(chatId));
                case "/journal" -> sendText(chatId, journalHandler.start(chatId));
                default -> sendText(chatId, aiHandler.ask(text));
            }
        }
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            switch (data) {
                case "MAIN_MENU" -> send(menuHandler.showMainMenu(chatId));
                case "CATALOG_BROWSE" -> send(catalogHandler.handleCatalogBrowse(chatId));
                case "ONBOARDING_CONSENT" -> sendText(chatId, onboardingHandler.start(chatId, update.getCallbackQuery().getFrom().getUserName()));
                default -> {
                    // simple routing
                    if (data.startsWith("CATALOG")) {
                        handleCatalogCallback(chatId, data);
                    } else if (data.startsWith("PLAN")) {
                        handlePlanCallback(chatId, data);
                    } else if (data.startsWith("SYMPTOM")) {
                        handleSymptomCallback(chatId, data);
                    }
                }
            }
        }
    }

    private void sendText(Long chatId, String text) {
        send(SendMessage.builder().chatId(chatId.toString()).text(text).build());
    }

    private void send(SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCatalogCallback(Long chatId, String data) {
        if (data.equals("CATALOG_BROWSE")) {
            send(catalogHandler.handleCatalogBrowse(chatId));
        } else if (data.equals("CATALOG:SEARCH")) {
            send(catalogHandler.promptSearch(chatId));
        } else if (data.startsWith("CATALOG:")) {
            String[] parts = data.split(":");
            if (parts.length >= 2 && parts[1].equals("ITEM")) {
                Long id = Long.parseLong(parts[2]);
                send(catalogHandler.handleProductDetails(chatId, id));
            } else if (parts.length >= 2 && parts[1].equals("SETS")) {
                send(catalogHandler.handleCategoryProducts(chatId, "sets", 0));
            }
        }
    }

    private void handlePlanCallback(Long chatId, String data) {
        if (data.startsWith("PLAN:DAY:")) {
            int day = Integer.parseInt(data.substring("PLAN:DAY:".length()));
            send(planHandler.showDayPlan(chatId, day));
        } else if (data.startsWith("PLAN:REGENERATE:")) {
            int day = Integer.parseInt(data.substring("PLAN:REGENERATE:".length()));
            send(planHandler.regenerateDay(chatId, day));
        } else if (data.equals("PLAN:WEEK")) {
            send(planHandler.showWeekPlan(chatId));
        }
    }

    private void handleSymptomCallback(Long chatId, String data) {
        if (data.equals("SYMPTOM:REDFLAG:YES")) {
            send(symptomsHandler.handleRedFlagAnswer(chatId, true));
        } else if (data.equals("SYMPTOM:REDFLAG:NO")) {
            send(symptomsHandler.handleRedFlagAnswer(chatId, false));
        } else if (data.startsWith("SYMPTOM:MONITOR:")) {
            Long id = Long.parseLong(data.substring("SYMPTOM:MONITOR:".length()));
            send(symptomsHandler.handleSymptomMonitoring(chatId, id));
        }
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }
}
