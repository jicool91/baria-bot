package com.baria.bot.handler;

import com.baria.bot.model.User;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanHandler {
    
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    
    // Временное хранилище планов (в продакшене использовать БД или Redis)
    private final Map<Long, WeeklyPlan> userPlans = new HashMap<>();
    
    public SendMessage generatePlan(Long chatId) {
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        
        if (userOpt.isEmpty()) {
            return createErrorMessage(chatId, "Пожалуйста, сначала пройдите регистрацию");
        }
        
        User user = userOpt.get();
        
        if (user.getPhase() == null) {
            return createErrorMessage(chatId, "Сначала необходимо определить вашу фазу питания");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("⏳ Генерирую персональный план питания на 7 дней...\n\nЭто может занять несколько секунд.");
        
        // Генерируем план
        WeeklyPlan plan = generateWeeklyPlan(user);
        userPlans.put(chatId, plan);
        
        // Показываем первый день
        return showDayPlan(chatId, 0);
    }
    
    public SendMessage showDayPlan(Long chatId, int dayIndex) {
        WeeklyPlan plan = userPlans.get(chatId);
        
        if (plan == null) {
            return generatePlan(chatId);
        }
        
        if (dayIndex < 0 || dayIndex >= 7) {
            dayIndex = 0;
        }
        
        DayPlan dayPlan = plan.days.get(dayIndex);
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        
        StringBuilder text = new StringBuilder();
        text.append("📅 ").append(dayPlan.dayName).append("\n");
        text.append("═══════════════════\n\n");
        
        // Завтрак
        text.append("🌅 ЗАВТРАК (").append(dayPlan.breakfast.time).append(")\n");
        text.append("• ").append(dayPlan.breakfast.meal).append("\n");
        text.append("  📊 Белок: ").append(dayPlan.breakfast.protein).append("г\n");
        text.append("  🥤 Объём: ").append(dayPlan.breakfast.volume).append("мл\n\n");
        
        // Перекус 1
        text.append("🥤 ПЕРЕКУС 1 (").append(dayPlan.snack1.time).append(")\n");
        text.append("• ").append(dayPlan.snack1.meal).append("\n");
        text.append("  📊 Белок: ").append(dayPlan.snack1.protein).append("г\n\n");
        
        // Обед
        text.append("☀️ ОБЕД (").append(dayPlan.lunch.time).append(")\n");
        text.append("• ").append(dayPlan.lunch.meal).append("\n");
        text.append("  📊 Белок: ").append(dayPlan.lunch.protein).append("г\n");
        text.append("  🥤 Объём: ").append(dayPlan.lunch.volume).append("мл\n\n");
        
        // Перекус 2
        text.append("🥤 ПЕРЕКУС 2 (").append(dayPlan.snack2.time).append(")\n");
        text.append("• ").append(dayPlan.snack2.meal).append("\n");
        text.append("  📊 Белок: ").append(dayPlan.snack2.protein).append("г\n\n");
        
        // Ужин
        text.append("🌙 УЖИН (").append(dayPlan.dinner.time).append(")\n");
        text.append("• ").append(dayPlan.dinner.meal).append("\n");
        text.append("  📊 Белок: ").append(dayPlan.dinner.protein).append("г\n");
        text.append("  🥤 Объём: ").append(dayPlan.dinner.volume).append("мл\n\n");
        
        // Итоги дня
        text.append("📊 ИТОГО ЗА ДЕНЬ:\n");
        text.append("• Белок: ").append(dayPlan.totalProtein).append("г\n");
        text.append("• Приёмов пищи: 5\n");
        
        if (userOpt.isPresent() && userOpt.get().getRestrictions() != null) {
            List<String> restrictions = userOpt.get().getRestrictions();
            if (!restrictions.isEmpty()) {
                text.append("\n✅ Учтены ограничения:\n");
                for (String restriction : restrictions) {
                    text.append("• ").append(getRestrictionDisplayName(restriction)).append("\n");
                }
            }
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text.toString());
        message.setReplyMarkup(createPlanNavigation(dayIndex));
        
        return message;
    }
    
    public SendMessage regenerateDay(Long chatId, int dayIndex) {
        WeeklyPlan plan = userPlans.get(chatId);
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        
        if (plan == null || userOpt.isEmpty()) {
            return generatePlan(chatId);
        }
        
        User user = userOpt.get();
        
        // Регенерируем только один день
        DayPlan newDay = generateDayPlan(user, dayIndex);
        plan.days.set(dayIndex, newDay);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("✅ План на " + newDay.dayName + " успешно обновлён!");
        
        // Показываем обновлённый день
        return showDayPlan(chatId, dayIndex);
    }
    
    private WeeklyPlan generateWeeklyPlan(User user) {
        WeeklyPlan plan = new WeeklyPlan();
        plan.userId = user.getId();
        plan.generatedAt = LocalDate.now();
        plan.days = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            plan.days.add(generateDayPlan(user, i));
        }
        
        return plan;
    }
    
    private DayPlan generateDayPlan(User user, int dayIndex) {
        String[] dayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        LocalDate date = LocalDate.now().plusDays(dayIndex);
        
        DayPlan day = new DayPlan();
        day.dayName = dayNames[dayIndex % 7] + ", " + date.format(DateTimeFormatter.ofPattern("dd.MM"));
        
        // Генерируем план через AI с учётом фазы и ограничений
        String prompt = buildPlanPrompt(user, dayIndex);
        String aiResponse = aiService.askQuestion(prompt, user);
        
        // Парсим ответ AI или используем шаблоны
        String phaseCode = user.getPhase() != null ? user.getPhase().getCode() : null;
        day = parsePlanOrUseTemplate(day, phaseCode, user.getRestrictions(), aiResponse);
        
        // Подсчитываем общий белок
        day.totalProtein = day.breakfast.protein + day.snack1.protein + 
                          day.lunch.protein + day.snack2.protein + day.dinner.protein;
        
        return day;
    }
    
    private String buildPlanPrompt(User user, int dayIndex) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Составь план питания на 1 день для пациента после бариатрической операции.\n");
        prompt.append("Фаза: ").append(user.getPhase() != null ? user.getPhase().getCode() : "unknown").append("\n");
        
        if (user.getRestrictions() != null && !user.getRestrictions().isEmpty()) {
            prompt.append("Ограничения: ").append(String.join(", ", user.getRestrictions())).append("\n");
        }
        
        prompt.append("Цель по белку: ").append(user.getGoal() != null ? user.getGoal() : "60-80г в день").append("\n");
        prompt.append("Формат: 5 приёмов пищи (завтрак, перекус, обед, перекус, ужин)\n");
        prompt.append("Укажи для каждого приёма: блюдо, количество белка, объём порции\n");
        
        return prompt.toString();
    }
    
    private DayPlan parsePlanOrUseTemplate(DayPlan day, String phase, List<String> restrictions, String aiResponse) {
        // Здесь должен быть парсинг ответа AI
        // Для примера используем готовые шаблоны по фазам
        
        boolean lactoseFree = restrictions != null && restrictions.contains("lactose_free");
        boolean vegetarian = restrictions != null && restrictions.contains("vegetarian");
        
        switch (phase) {
            case "liquid":
                day.breakfast = new Meal("Протеиновый коктейль", "09:00", 20, 150);
                day.snack1 = new Meal("Бульон куриный", "11:00", 5, 150);
                day.lunch = new Meal("Крем-суп из тыквы", "13:00", 10, 200);
                day.snack2 = new Meal("Йогурт питьевой " + (lactoseFree ? "безлактозный" : ""), "16:00", 8, 150);
                day.dinner = new Meal("Смузи с протеином", "19:00", 15, 200);
                break;
                
            case "pureed":
                day.breakfast = new Meal("Творожное пюре " + (lactoseFree ? "безлактозное" : ""), "09:00", 18, 120);
                day.snack1 = new Meal("Протеиновый пудинг", "11:00", 12, 100);
                day.lunch = new Meal(vegetarian ? "Пюре из чечевицы" : "Пюре из курицы с овощами", "13:00", 20, 150);
                day.snack2 = new Meal("Яблочное пюре с протеином", "16:00", 8, 120);
                day.dinner = new Meal(vegetarian ? "Тофу-пюре" : "Рыбное пюре с брокколи", "19:00", 18, 150);
                break;
                
            case "soft":
                day.breakfast = new Meal("Яичница-болтунья", "09:00", 14, 120);
                day.snack1 = new Meal("Мягкий сыр " + (lactoseFree ? "безлактозный" : ""), "11:00", 10, 50);
                day.lunch = new Meal(vegetarian ? "Тушёная фасоль" : "Тефтели в соусе", "13:00", 22, 180);
                day.snack2 = new Meal("Греческий йогурт", "16:00", 12, 120);
                day.dinner = new Meal(vegetarian ? "Омлет с овощами" : "Запечённая рыба", "19:00", 20, 150);
                break;
                
            case "regular":
            default:
                day.breakfast = new Meal("Овсянка с ягодами и протеином", "09:00", 15, 200);
                day.snack1 = new Meal("Протеиновый батончик", "11:00", 12, 0);
                day.lunch = new Meal(vegetarian ? "Салат с киноа" : "Куриная грудка с овощами", "13:00", 25, 250);
                day.snack2 = new Meal("Орехи и сухофрукты", "16:00", 8, 0);
                day.dinner = new Meal(vegetarian ? "Паста с тофу" : "Стейк из лосося с рисом", "19:00", 22, 250);
                break;
        }
        
        return day;
    }
    
    private InlineKeyboardMarkup createPlanNavigation(int currentDay) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Навигация по дням
        List<InlineKeyboardButton> navigation = new ArrayList<>();
        
        if (currentDay > 0) {
            InlineKeyboardButton prevBtn = new InlineKeyboardButton();
            prevBtn.setText("◀️ Пред. день");
            prevBtn.setCallbackData("PLAN:DAY:" + (currentDay - 1));
            navigation.add(prevBtn);
        }
        
        InlineKeyboardButton currentBtn = new InlineKeyboardButton();
        currentBtn.setText("День " + (currentDay + 1) + "/7");
        currentBtn.setCallbackData("PLAN:CURRENT");
        navigation.add(currentBtn);
        
        if (currentDay < 6) {
            InlineKeyboardButton nextBtn = new InlineKeyboardButton();
            nextBtn.setText("След. день ▶️");
            nextBtn.setCallbackData("PLAN:DAY:" + (currentDay + 1));
            navigation.add(nextBtn);
        }
        
        keyboard.add(navigation);
        
        // Действия
        InlineKeyboardButton regenerateBtn = new InlineKeyboardButton();
        regenerateBtn.setText("🔄 Перегенерировать день");
        regenerateBtn.setCallbackData("PLAN:REGENERATE:" + currentDay);
        keyboard.add(Arrays.asList(regenerateBtn));
        
        InlineKeyboardButton weekBtn = new InlineKeyboardButton();
        weekBtn.setText("📅 Вся неделя");
        weekBtn.setCallbackData("PLAN:WEEK");
        keyboard.add(Arrays.asList(weekBtn));
        
        InlineKeyboardButton productsBtn = new InlineKeyboardButton();
        productsBtn.setText("🛒 Рекомендуемые продукты");
        productsBtn.setCallbackData("CATALOG:FILTERED");
        keyboard.add(Arrays.asList(productsBtn));
        
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Главное меню");
        backBtn.setCallbackData("MAIN_MENU");
        keyboard.add(Arrays.asList(backBtn));
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    public SendMessage showWeekPlan(Long chatId) {
        WeeklyPlan plan = userPlans.get(chatId);
        
        if (plan == null) {
            return generatePlan(chatId);
        }
        
        StringBuilder text = new StringBuilder();
        text.append("📅 ПЛАН ПИТАНИЯ НА НЕДЕЛЮ\n");
        text.append("════════════════════════\n\n");
        
        for (int i = 0; i < plan.days.size(); i++) {
            DayPlan day = plan.days.get(i);
            text.append(day.dayName).append(":\n");
            text.append("• Белок: ").append(day.totalProtein).append("г\n");
            text.append("• Основные блюда: ")
                .append(day.breakfast.meal.split(" ")[0]).append(", ")
                .append(day.lunch.meal.split(" ")[0]).append(", ")
                .append(day.dinner.meal.split(" ")[0]).append("\n\n");
        }
        
        text.append("💡 Нажмите на день для подробного просмотра");
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text.toString());
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопки для каждого дня
        for (int i = 0; i < 7; i++) {
            InlineKeyboardButton dayBtn = new InlineKeyboardButton();
            dayBtn.setText((i + 1) + ". " + plan.days.get(i).dayName.split(",")[0]);
            dayBtn.setCallbackData("PLAN:DAY:" + i);
            keyboard.add(Arrays.asList(dayBtn));
        }
        
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Главное меню");
        backBtn.setCallbackData("MAIN_MENU");
        keyboard.add(Arrays.asList(backBtn));
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    private SendMessage createErrorMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("❌ " + text);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Главное меню");
        backBtn.setCallbackData("MAIN_MENU");
        keyboard.add(Arrays.asList(backBtn));
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    private String getRestrictionDisplayName(String restriction) {
        switch (restriction) {
            case "lactose_free": return "Без лактозы";
            case "gluten_free": return "Без глютена";
            case "vegetarian": return "Вегетарианство";
            case "vegan": return "Веганство";
            case "halal": return "Халяль";
            case "kosher": return "Кошер";
            default: return restriction;
        }
    }
    
    // Вспомогательные классы
    private static class WeeklyPlan {
        Long userId;
        LocalDate generatedAt;
        List<DayPlan> days;
    }
    
    private static class DayPlan {
        String dayName;
        Meal breakfast;
        Meal snack1;
        Meal lunch;
        Meal snack2;
        Meal dinner;
        int totalProtein;
    }
    
    private static class Meal {
        String meal;
        String time;
        int protein;
        int volume;
        
        Meal(String meal, String time, int protein, int volume) {
            this.meal = meal;
            this.time = time;
            this.protein = protein;
            this.volume = volume;
        }
    }
}
