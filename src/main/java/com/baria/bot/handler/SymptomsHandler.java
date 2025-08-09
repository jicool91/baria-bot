package com.baria.bot.handler;

import com.baria.bot.model.RedFlag;
import com.baria.bot.model.Reminder;
import com.baria.bot.model.SymptomLog;
import com.baria.bot.model.User;
import com.baria.bot.repository.RedFlagRepository;
import com.baria.bot.repository.SymptomLogRepository;
import com.baria.bot.repository.UserRepository;
import com.baria.bot.service.AiService;
import com.baria.bot.service.RedFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SymptomsHandler {
    
    private final RedFlagService redFlagService;
    private final RedFlagRepository redFlagRepository;
    private final SymptomLogRepository symptomLogRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final com.baria.bot.service.ReminderService reminderService;
    
    // Карта временных данных для триажа (в продакшене лучше использовать Redis)
    private final Map<Long, TriageSession> triageSessions = new HashMap<>();
    
    public SendMessage handleSymptomCheck(Long chatId, String symptom) {
        // Начинаем сессию триажа
        TriageSession session = new TriageSession();
        session.symptom = symptom;
        session.currentStep = 0;
        triageSessions.put(chatId, session);
        
        return sendRedFlagQuestion(chatId, session, 0);
    }
    
    public SendMessage handleRedFlagAnswer(Long chatId, boolean answer) {
        TriageSession session = triageSessions.get(chatId);
        if (session == null) {
            return createMainMenuMessage(chatId, "Сессия истекла. Пожалуйста, начните заново.");
        }
        
        if (answer) {
            // Red flag обнаружен - экстренная помощь
            triageSessions.remove(chatId);
            return createUrgentCareMessage(chatId, session.symptom);
        }
        
        session.currentStep++;
        
        List<RedFlag> redFlags = redFlagRepository.findByIsActiveTrue();
        if (session.currentStep < redFlags.size()) {
            return sendRedFlagQuestion(chatId, session, session.currentStep);
        } else {
            // Все red flags пройдены, нет опасности
            triageSessions.remove(chatId);
            return handleNonUrgentSymptom(chatId, session.symptom);
        }
    }
    
    private SendMessage sendRedFlagQuestion(Long chatId, TriageSession session, int questionIndex) {
        List<RedFlag> redFlags = redFlagRepository.findByIsActiveTrue();
        
        if (questionIndex >= redFlags.size()) {
            return handleNonUrgentSymptom(chatId, session.symptom);
        }
        
        RedFlag redFlag = redFlags.get(questionIndex);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("⚠️ Важный вопрос для оценки состояния:\n\n" + redFlag.getQuestion());
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        InlineKeyboardButton yesBtn = new InlineKeyboardButton();
        yesBtn.setText("✅ Да");
        yesBtn.setCallbackData("SYMPTOM:REDFLAG:YES");
        
        InlineKeyboardButton noBtn = new InlineKeyboardButton();
        noBtn.setText("❌ Нет");
        noBtn.setCallbackData("SYMPTOM:REDFLAG:NO");
        
        keyboard.add(Arrays.asList(yesBtn, noBtn));
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    private SendMessage createUrgentCareMessage(Long chatId, String symptom) {
        // Логируем критический симптом
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        if (userOpt.isPresent()) {
            SymptomLog log = new SymptomLog();
            log.setUser(userOpt.get());
            log.setSymptom(symptom);
            log.setStartedAt(LocalDateTime.now());
            log.setStatus("urgent");
            log.setNotes("Red flag detected - advised urgent care");
            symptomLogRepository.save(log);
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(
            "🚨 ТРЕБУЕТСЯ СРОЧНАЯ МЕДИЦИНСКАЯ ПОМОЩЬ\n\n" +
            "Ваши симптомы могут указывать на серьёзное состояние, требующее немедленного медицинского вмешательства.\n\n" +
            "❗ Я не ставлю диагнозы, но настоятельно рекомендую:\n\n" +
            "1. Немедленно обратиться за медицинской помощью\n" +
            "2. Вызвать скорую помощь по телефону 103 или 112\n" +
            "3. Обратиться в ближайшую больницу\n\n" +
            "Не откладывайте обращение к врачу!"
        );
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        InlineKeyboardButton emergencyBtn = new InlineKeyboardButton();
        emergencyBtn.setText("📞 Вызвать 103");
        emergencyBtn.setUrl("tel:103");
        keyboard.add(Arrays.asList(emergencyBtn));
        
        InlineKeyboardButton hospitalsBtn = new InlineKeyboardButton();
        hospitalsBtn.setText("📍 Ближайшие клиники");
        hospitalsBtn.setUrl("https://maps.google.com/maps/search/больница+рядом");
        keyboard.add(Arrays.asList(hospitalsBtn));
        
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Главное меню");
        backBtn.setCallbackData("MAIN_MENU");
        keyboard.add(Arrays.asList(backBtn));
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    private SendMessage handleNonUrgentSymptom(Long chatId, String symptom) {
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        
        // Логируем симптом
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            SymptomLog log = new SymptomLog();
            log.setUser(user);
            log.setSymptom(symptom);
            log.setStartedAt(LocalDateTime.now());
            log.setStatus("active");
            log.setNotes("Non-urgent symptom");
            symptomLogRepository.save(log);
            
            // Получаем ИИ-рекомендации через RAG
            String phase = user.getPhase() != null ? user.getPhase().getCode() : "unknown";
            String aiResponse = aiService.askQuestion(
                "Пациент после бариатрической операции, фаза " + phase + 
                ", жалуется на: " + symptom + 
                ". Дай краткие рекомендации по самопомощи БЕЗ лекарств.",
                user
            );
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(
                "📋 Рекомендации по симптому: " + symptom + "\n\n" +
                aiResponse + "\n\n" +
                "⚠️ Это информационная справка, не медицинская рекомендация.\n" +
                "При ухудшении состояния обязательно обратитесь к врачу.\n\n" +
                "Рекомендую наблюдать за симптомом в течение 24-48 часов."
            );
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            InlineKeyboardButton monitorBtn = new InlineKeyboardButton();
            monitorBtn.setText("🔔 Включить мониторинг 48ч");
            monitorBtn.setCallbackData("SYMPTOM:MONITOR:" + log.getId());
            keyboard.add(Arrays.asList(monitorBtn));
            
            InlineKeyboardButton memoBtn = new InlineKeyboardButton();
            memoBtn.setText("📝 Памятка для врача");
            memoBtn.setCallbackData("SYMPTOM:MEMO");
            keyboard.add(Arrays.asList(memoBtn));
            
            InlineKeyboardButton backBtn = new InlineKeyboardButton();
            backBtn.setText("⬅️ Главное меню");
            backBtn.setCallbackData("MAIN_MENU");
            keyboard.add(Arrays.asList(backBtn));
            
            markup.setKeyboard(keyboard);
            message.setReplyMarkup(markup);
            
            return message;
        } else {
            return createMainMenuMessage(chatId, "Пожалуйста, сначала пройдите регистрацию.");
        }
    }
    
    public SendMessage handleSymptomMonitoring(Long chatId, Long symptomLogId) {
        Optional<SymptomLog> logOpt = symptomLogRepository.findById(symptomLogId);
        
        if (logOpt.isEmpty()) {
            return createMainMenuMessage(chatId, "Запись о симптоме не найдена");
        }
        
        SymptomLog log = logOpt.get();
        log.setNotes(log.getNotes() + "; Monitoring enabled for 48h");
        symptomLogRepository.save(log);

        reminderService.createReminder(Reminder.builder()
                .user(log.getUser())
                .type(Reminder.ReminderType.SYMPTOM_WATCH)
                .rrule("FREQ=HOURLY;INTERVAL=12;COUNT=4")
                .localTime("00:00")
                .meta(java.util.Map.of("symptom", log.getSymptom()))
                .build());
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(
            "✅ Мониторинг симптома включён!\n\n" +
            "Я буду напоминать вам проверять состояние каждые 12 часов в течение следующих 48 часов.\n\n" +
            "Если симптом усилится или появятся новые тревожные признаки - немедленно обратитесь к врачу."
        );
        
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
    
    public SendMessage generateSymptomMemo(Long chatId) {
        Optional<User> userOpt = userRepository.findByTgId(chatId);
        
        StringBuilder memo = new StringBuilder();
        memo.append("📝 ПАМЯТКА ДЛЯ ВИЗИТА К ВРАЧУ\n");
        memo.append("================================\n\n");
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            memo.append("Дата операции: ").append(user.getSurgeryDate()).append("\n");
            memo.append("Текущая фаза: ").append(getPhaseDisplayName(user.getPhase().getCode())).append("\n");
            memo.append("Рост: ").append(user.getHeightCm()).append(" см\n");
            memo.append("Вес: ").append(user.getWeightKg()).append(" кг\n\n");
            
            // Последние симптомы
            List<SymptomLog> recentSymptoms = symptomLogRepository.findByUserAndStatusOrderByStartedAtDesc(
                user, "active"
            );
            
            if (!recentSymptoms.isEmpty()) {
                memo.append("ПОСЛЕДНИЕ СИМПТОМЫ:\n");
                for (SymptomLog symptom : recentSymptoms) {
                    memo.append("• ").append(symptom.getSymptom())
                        .append(" (начало: ").append(symptom.getStartedAt()).append(")\n");
                }
            }
        }
        
        memo.append("\nВОПРОСЫ К ВРАЧУ:\n");
        memo.append("• _____________________\n");
        memo.append("• _____________________\n");
        memo.append("• _____________________\n");
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(memo.toString());
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Назад");
        backBtn.setCallbackData("MAIN_MENU");
        keyboard.add(Arrays.asList(backBtn));
        
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    private SendMessage createMainMenuMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
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
    
    private String getPhaseDisplayName(String phase) {
        if (phase == null) return "Не определена";
        switch (phase) {
            case "liquid": return "Жидкая";
            case "pureed": return "Пюре";
            case "soft": return "Мягкая";
            case "regular": return "Обычная";
            default: return phase;
        }
    }
    
    // Внутренний класс для хранения сессии триажа
    private static class TriageSession {
        String symptom;
        int currentStep;
        List<Boolean> answers = new ArrayList<>();
    }
}
