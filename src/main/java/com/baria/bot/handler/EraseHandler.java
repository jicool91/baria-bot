package com.baria.bot.handler;

import com.baria.bot.service.ProfileService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class EraseHandler {
    private final ProfileService profileService;

    public EraseHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    public SendMessage erase(Long chatId) {
        profileService.deleteUser(chatId);
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText("Ваши данные удалены. Чтобы начать заново, используйте /start");
        return m;
    }
}
