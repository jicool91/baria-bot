package com.baria.bot.handler;

import com.baria.bot.model.User;
import com.baria.bot.model.User.Goal;
import com.baria.bot.model.User.Phase;
import com.baria.bot.model.User.PhaseMode;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.OnboardingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class OnboardingHandler {
    private final OnboardingService onboardingService;
    private final UserRepository userRepository;
    private final Map<Long, OnboardingStep> steps = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public OnboardingHandler(OnboardingService onboardingService, UserRepository userRepository) {
        this.onboardingService = onboardingService;
        this.userRepository = userRepository;
    }

    public boolean isOnboarding(Long chatId) {
        return steps.getOrDefault(chatId, OnboardingStep.NONE) != OnboardingStep.NONE;
    }

    public String start(Long chatId, String username) {
        onboardingService.getOrCreate(chatId, username);
        steps.put(chatId, OnboardingStep.CONSENT);
        return "Нужен ваш согласие на обработку данных.\nНапишите 'согласен' для продолжения.";
    }

    public String handle(Long chatId, String text) {
        OnboardingStep step = steps.getOrDefault(chatId, OnboardingStep.NONE);
        switch (step) {
            case CONSENT -> {
                if (text.equalsIgnoreCase("согласен")) {
                    onboardingService.saveConsent(chatId);
                    steps.put(chatId, OnboardingStep.ONBOARDING_DATE);
                    return "Введите дату операции (ДД.ММ.ГГГГ)";
                }
                return "Чтобы продолжить, напишите 'согласен'";
            }
            case ONBOARDING_DATE -> {
                try {
                    LocalDate date = LocalDate.parse(text.trim(), formatter);
                    onboardingService.saveSurgeryDate(chatId, date);
                    steps.put(chatId, OnboardingStep.ONBOARDING_MEASURES);
                    return "Введите рост (см), вес (кг) и цель (lose/maintain/protein) через запятую";
                } catch (Exception e) {
                    return "Неверный формат даты";
                }
            }
            case ONBOARDING_MEASURES -> {
                String[] parts = text.split(",");
                if (parts.length < 3) {
                    return "Формат: рост, вес, цель";
                }
                try {
                    int height = Integer.parseInt(parts[0].trim());
                    BigDecimal weight = new BigDecimal(parts[1].trim());
                    Goal goal = Goal.fromCode(parts[2].trim());
                    onboardingService.saveMeasures(chatId, height, weight, goal);
                    steps.put(chatId, OnboardingStep.ONBOARDING_PHASE);
                    return "Укажите текущую фазу (liquid/pureed/soft/regular) или 'авто'";
                } catch (Exception e) {
                    return "Не удалось распознать данные";
                }
            }
            case ONBOARDING_PHASE -> {
                String value = text.trim().toLowerCase();
                PhaseMode mode = PhaseMode.MANUAL;
                Phase phase = null;
                if (value.equals("авто")) {
                    mode = PhaseMode.AUTO;
                } else {
                    phase = Phase.fromCode(value);
                }
                onboardingService.savePhase(chatId, phase, mode);
                steps.put(chatId, OnboardingStep.ONBOARDING_RESTRICTIONS);
                return "Перечислите пищевые ограничения через запятую или напишите 'нет'";
            }
            case ONBOARDING_RESTRICTIONS -> {
                List<String> list = Collections.emptyList();
                if (!text.equalsIgnoreCase("нет")) {
                    list = Arrays.stream(text.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }
                onboardingService.saveRestrictions(chatId, list);
                steps.put(chatId, OnboardingStep.ONBOARDING_SYMPTOMS);
                return "Есть ли симптомы сейчас? перечислите через запятую или напишите 'нет'";
            }
            case ONBOARDING_SYMPTOMS -> {
                List<String> list = Collections.emptyList();
                if (!text.equalsIgnoreCase("нет")) {
                    list = Arrays.stream(text.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }
                onboardingService.saveSymptoms(chatId, list);
                steps.put(chatId, OnboardingStep.ONBOARDING_TZ);
                return "Укажите ваш часовой пояс (пример Europe/Moscow)";
            }
            case ONBOARDING_TZ -> {
                onboardingService.saveTimezone(chatId, text.trim());
                steps.put(chatId, OnboardingStep.ONBOARDING_SUPPS);
                return "Принимаете ли витамины/добавки? перечислите через запятую или 'нет'";
            }
            case ONBOARDING_SUPPS -> {
                List<String> list = Collections.emptyList();
                if (!text.equalsIgnoreCase("нет")) {
                    list = Arrays.stream(text.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }
                onboardingService.saveSupplements(chatId, list);
                steps.put(chatId, OnboardingStep.ONBOARDING_SUMMARY);
                return buildSummary(chatId);
            }
            case ONBOARDING_SUMMARY -> {
                steps.remove(chatId);
                return "Онбординг завершён!";
            }
            default -> {
                return "";
            }
        }
    }

    private String buildSummary(Long chatId) {
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        if (userOpt.isEmpty()) return "Ошибка";
        User u = userOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Профиль создан!\n");
        sb.append("Фаза: ").append(u.getPhase() != null ? u.getPhase().getDisplayName() : "авто").append("\n");
        if (u.getRestrictions() != null && !u.getRestrictions().isEmpty()) {
            sb.append("Ограничения: ").append(String.join(", ", u.getRestrictions())).append("\n");
        }
        sb.append("Цель: ").append(u.getGoal() != null ? u.getGoal().getDisplayName() : "не указана");
        sb.append("\n\nДоступные действия:\n/plan - план питания\n/reminders - напоминания\n/products - каталог");
        return sb.toString();
    }
}
