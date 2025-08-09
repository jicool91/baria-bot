package com.baria.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class MenuHandler {
    public SendMessage showMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Привет! Я помогу с продукцией компании и сопровождением после бариатрической операции.\nЧто выберете?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton catalogBtn = new InlineKeyboardButton();
        catalogBtn.setText("\uD83D\uDED2 Узнать о продукции");
        catalogBtn.setCallbackData("CATALOG_BROWSE");
        keyboard.add(List.of(catalogBtn));

        InlineKeyboardButton onboardingBtn = new InlineKeyboardButton();
        onboardingBtn.setText("\uD83E\uDE7A Я после операции");
        onboardingBtn.setCallbackData("ONBOARDING_CONSENT");
        keyboard.add(List.of(onboardingBtn));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        return message;
    }
}
