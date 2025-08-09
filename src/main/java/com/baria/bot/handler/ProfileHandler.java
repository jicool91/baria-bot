package com.baria.bot.handler;

import com.baria.bot.model.User;
import com.baria.bot.service.ProfileService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ProfileHandler {
    private final ProfileService profileService;
    private final Set<Long> editingRestrictions = new HashSet<>();

    public ProfileHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    public boolean isEditingRestrictions(Long chatId) {
        return editingRestrictions.contains(chatId);
    }

    public String startEditing(Long chatId) {
        editingRestrictions.add(chatId);
        return "Укажите ваши пищевые ограничения или напишите 'нет'";
    }

    public String saveRestrictions(Long chatId, String text) {
        editingRestrictions.remove(chatId);
        if (text.equalsIgnoreCase("нет")) {
            profileService.updateRestrictions(chatId, null);
            return "Ограничения: отсутствуют";
        }
        List<String> list = Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        profileService.updateRestrictions(chatId, list);
        return "Ограничения обновлены!";
    }

    public String showProfile(Long chatId) {
        Optional<User> opt = profileService.getByTgId(chatId);
        if (opt.isEmpty()) {
            return "Профиль не найден";
        }
        User u = opt.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String surgery = u.getSurgeryDate() != null ? u.getSurgeryDate().format(fmt) : "Не указана";
        String phase = u.getPhase() != null ? u.getPhase().getDisplayName() : "Не указана";
        String restr = (u.getRestrictions() != null && !u.getRestrictions().isEmpty()) ? String.join(", ", u.getRestrictions()) : "Не указаны";
        return String.format(
                "\uD83D\uDC64 %s\n\uD83D\uDCC5 Операция: %s\n\uD83C\uDF7D\uFE0F Ограничения: %s\n\uD83C\uDF56 Фаза: %s",
                u.getFullName(), surgery, restr, phase);
    }
}

