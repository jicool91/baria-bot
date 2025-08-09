package com.baria.bot.handler;

import com.baria.bot.model.User;
import com.baria.bot.service.OnboardingService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class OnboardingHandler {
    private final OnboardingService onboardingService;
    private final Map<Long, OnboardingStep> steps = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public OnboardingHandler(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    public boolean isOnboarding(Long chatId) {
        return steps.getOrDefault(chatId, OnboardingStep.NONE) != OnboardingStep.NONE;
    }

    public String start(Long chatId, String username) {
        User user = onboardingService.getOrCreate(chatId, username);
        if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            return "Вы уже зарегистрированы";
        }
        steps.put(chatId, OnboardingStep.BASIC_INFO);
        return "\uD83D\uDC4B **Добро пожаловать в бариатрический бот!**\n\n" +
                "Я помогу вам в восстановлении после операции.\n\n" +
                "**Шаг 1 из 3:** Основная информация\n\n" +
                "Введите через запятую: имя и дату операции (ДД.ММ.ГГГГ)\n" +
                "**Пример:** Анна Петрова, 15.06.2024";
    }

    public String handle(Long chatId, String text) {
        OnboardingStep step = steps.getOrDefault(chatId, OnboardingStep.NONE);
        switch (step) {
            case BASIC_INFO -> {
                String[] parts = text.split(",");
                if (parts.length != 2) {
                    return "❌ Неверный формат. Пример: Анна Петрова, 15.06.2024";
                }
                try {
                    String name = parts[0].trim();
                    LocalDate date = LocalDate.parse(parts[1].trim(), formatter);
                    onboardingService.saveBasicInfo(chatId, name, date);
                    steps.put(chatId, OnboardingStep.DOCTOR_CODE);
                    return "✅ Данные сохранены!\n\nШаг 2 из 3: введите код врача";
                } catch (Exception e) {
                    return "❌ Неверный формат даты";
                }
            }
            case DOCTOR_CODE -> {
                String doctor = onboardingService.saveDoctorCode(chatId, text.trim()).orElse(null);
                if (doctor == null) {
                    return "❌ Код не найден. Попробуйте снова";
                }
                steps.put(chatId, OnboardingStep.CONSENT);
                return "✅ Код принят!\n\n\uD83D\uDC68\u200D⚕️ Ваш врач: " + doctor +
                        "\n\nШаг 3 из 3: напишите 'согласен' для завершения";
            }
            case CONSENT -> {
                if (text.equalsIgnoreCase("согласен")) {
                    onboardingService.saveConsent(chatId);
                    steps.remove(chatId);
                    return "🎉 Регистрация завершена!\n\nИспользуйте меню ниже";
                }
                return "Напишите 'согласен' для завершения";
            }
            default -> {
                return "";
            }
        }
    }
}

