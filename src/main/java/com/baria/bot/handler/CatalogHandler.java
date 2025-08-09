package com.baria.bot.handler;

import com.baria.bot.model.Product;
import com.baria.bot.model.User;
import com.baria.bot.repository.ProductRepository;
import com.baria.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogHandler {
    
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final java.util.Set<Long> searchAwait = new java.util.HashSet<>();

    private static final int PAGE_SIZE = 5;
    
    public SendMessage handleCatalogBrowse(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите раздел:");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Категории продуктов
        keyboard.add(createButton("🍽 Наборы еды для диет", "CATALOG:SETS"));
        keyboard.add(createButton("🎭 Маски", "CATALOG:MASKS"));
        keyboard.add(createButton("🧴 Шампуни", "CATALOG:SHAMPOOS"));
        keyboard.add(createButton("🧴 Кремы", "CATALOG:CREAMS"));
        keyboard.add(createButton("💊 Витамины и добавки", "CATALOG:SUPPS"));
        keyboard.add(createButton("🔎 Поиск", "CATALOG:SEARCH"));
        keyboard.add(createButton("⬅️ Назад", "MAIN_MENU"));
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }

    public boolean isAwaitingSearch(Long chatId) {
        return searchAwait.contains(chatId);
    }

    public SendMessage promptSearch(Long chatId) {
        searchAwait.add(chatId);
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText("Введите название или фильтры для поиска товара");
        return m;
    }

    public SendMessage handleSearch(Long chatId, String query) {
        searchAwait.remove(chatId);
        Page<Product> products = productRepository.searchProducts(query, PageRequest.of(0, PAGE_SIZE));
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        if (products.isEmpty()) {
            message.setText("Ничего не найдено");
            message.setReplyMarkup(createBackButton());
            return message;
        }
        StringBuilder text = new StringBuilder("Найдено:\n\n");
        for (Product p : products.getContent()) {
            text.append("• ").append(p.getTitle()).append("\n");
        }
        message.setText(text.toString());
        message.setReplyMarkup(createProductListMarkup(products, "SEARCH"));
        return message;
    }
    
    public SendMessage handleCategoryProducts(Long chatId, String category, int page) {
        Page<Product> products = productRepository.findByCategory(category, PageRequest.of(page, PAGE_SIZE));
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        
        if (products.isEmpty()) {
            message.setText("В этой категории пока нет товаров");
            message.setReplyMarkup(createBackButton());
            return message;
        }
        
        StringBuilder text = new StringBuilder("📦 Товары в категории:\n\n");
        
        for (Product product : products.getContent()) {
            text.append("• ").append(product.getTitle()).append("\n");
            text.append("  ").append(product.getDescription()).append("\n");
            
            // Добавляем бейджи
            if (product.getAllowedPhases() != null && !product.getAllowedPhases().isEmpty()) {
                text.append("  📊 Фазы: ").append(String.join(", ", product.getAllowedPhases())).append("\n");
            }
            if (product.getTags() != null && product.getTags().contains("lactose_free")) {
                text.append("  🥛 Без лактозы\n");
            }
            text.append("\n");
        }
        
        message.setText(text.toString());
        message.setReplyMarkup(createProductListMarkup(products, category));
        
        return message;
    }
    
    public SendMessage handleProductDetails(Long chatId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Товар не найден");
            message.setReplyMarkup(createBackButton());
            return message;
        }
        
        Product product = productOpt.get();
        
        StringBuilder text = new StringBuilder();
        text.append("📦 ").append(product.getTitle()).append("\n\n");
        text.append("📝 Описание:\n").append(product.getDescription()).append("\n\n");
        
        if (product.getPrice() != null) {
            text.append("💰 Цена: ").append(product.getPrice()).append(" ₽\n\n");
        }
        
        if (product.getAllowedPhases() != null && !product.getAllowedPhases().isEmpty()) {
            text.append("📊 Разрешён в фазах:\n");
            for (String phase : product.getAllowedPhases()) {
                text.append("• ").append(getPhaseDisplayName(phase)).append("\n");
            }
            text.append("\n");
        }
        
        if (product.getAllergens() != null && !product.getAllergens().isEmpty()) {
            text.append("⚠️ Аллергены: ").append(String.join(", ", product.getAllergens())).append("\n\n");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text.toString());
        message.setReplyMarkup(createProductDetailsMarkup(product, chatId));
        
        return message;
    }
    
    public SendMessage checkProductPhase(Long chatId, Long productId) {
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        Optional<Product> productOpt = productRepository.findById(productId);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        
        if (userOpt.isEmpty() || userOpt.get().getPhase() == null) {
            message.setText("❌ Сначала пройдите онбординг, чтобы определить вашу фазу питания");
            message.setReplyMarkup(createBackButton());
            return message;
        }
        
        if (productOpt.isEmpty()) {
            message.setText("Товар не найден");
            message.setReplyMarkup(createBackButton());
            return message;
        }
        
        User user = userOpt.get();
        Product product = productOpt.get();
        
        if (product.getAllowedPhases() == null || product.getAllowedPhases().isEmpty()) {
            message.setText("ℹ️ Для этого товара не указаны ограничения по фазам");
        } else if (product.getAllowedPhases().contains(user.getPhase().getCode())) {
            message.setText("✅ Этот товар разрешён в вашей фазе (" + getPhaseDisplayName(user.getPhase().getCode()) + ")");
        } else {
            message.setText("❌ Этот товар НЕ рекомендован в вашей фазе (" + getPhaseDisplayName(user.getPhase().getCode()) + ").\n" +
                          "Разрешён в фазах: " + String.join(", ", product.getAllowedPhases()));
        }
        
        message.setReplyMarkup(createBackButton());
        return message;
    }
    
    private InlineKeyboardMarkup createProductListMarkup(Page<Product> products, String category) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопки товаров
        for (Product product : products.getContent()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton detailsBtn = new InlineKeyboardButton();
            detailsBtn.setText("ℹ️ " + product.getTitle());
            detailsBtn.setCallbackData("CATALOG:ITEM:" + product.getId());
            row.add(detailsBtn);
            
            if (product.getUrl() != null) {
                InlineKeyboardButton buyBtn = new InlineKeyboardButton();
                buyBtn.setText("🛒 Купить");
                buyBtn.setUrl(product.getUrl());
                row.add(buyBtn);
            }
            
            keyboard.add(row);
        }
        
        // Пагинация
        List<InlineKeyboardButton> pagination = new ArrayList<>();
        if (products.hasPrevious()) {
            InlineKeyboardButton prevBtn = new InlineKeyboardButton();
            prevBtn.setText("◀️ Пред");
            prevBtn.setCallbackData("CATALOG:" + category + ":PAGE:" + (products.getNumber() - 1));
            pagination.add(prevBtn);
        }
        
        if (products.hasNext()) {
            InlineKeyboardButton nextBtn = new InlineKeyboardButton();
            nextBtn.setText("▶️ След");
            nextBtn.setCallbackData("CATALOG:" + category + ":PAGE:" + (products.getNumber() + 1));
            pagination.add(nextBtn);
        }
        
        if (!pagination.isEmpty()) {
            keyboard.add(pagination);
        }
        
        keyboard.add(createButton("⬅️ Назад", "CATALOG_BROWSE"));
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    private InlineKeyboardMarkup createProductDetailsMarkup(Product product, Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        if (product.getUrl() != null) {
            InlineKeyboardButton buyBtn = new InlineKeyboardButton();
            buyBtn.setText("🛒 Купить");
            buyBtn.setUrl(product.getUrl());
            keyboard.add(Arrays.asList(buyBtn));
        }
        
        keyboard.add(createButton("✅ Разрешён в моей фазе?", "CATALOG:CHECK_PHASE:" + product.getId()));
        keyboard.add(createButton("⬅️ Назад", "CATALOG:" + product.getCategory()));
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    private InlineKeyboardMarkup createBackButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButton("⬅️ Назад", "CATALOG_BROWSE"));
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    private List<InlineKeyboardButton> createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return Arrays.asList(button);
    }
    
    private String getPhaseDisplayName(String phase) {
        switch (phase) {
            case "liquid": return "Жидкая";
            case "pureed": return "Пюре";
            case "soft": return "Мягкая";
            case "regular": return "Обычная";
            default: return phase;
        }
    }
}
