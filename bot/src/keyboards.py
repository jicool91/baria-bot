from aiogram.types import ReplyKeyboardMarkup, KeyboardButton

# Обновленное главное меню с кнопкой профиля
main_patient_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="🤖 Вопрос ИИ"), KeyboardButton(text="📝 Журнал")],
        [KeyboardButton(text="📅 План питания"), KeyboardButton(text="👤 Профиль")],
        [KeyboardButton(text="💊 Витамины"), KeyboardButton(text="🩺 Связь с врачом")]
    ], resize_keyboard=True
)

# Клавиатура врача (без изменений)
main_doctor_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="👥 Мои пациенты"), KeyboardButton(text="🗓 Записи пациентов")],
        [KeyboardButton(text="📨 Входящие вопросы")]
    ], resize_keyboard=True
)

# Клавиатура профиля (для будущего использования)
profile_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="📊 Мои данные"), KeyboardButton(text="✏️ Редактировать")],
        [KeyboardButton(text="📈 Статистика"), KeyboardButton(text="🔙 Главное меню")]
    ], resize_keyboard=True
)

# Клавиатура для быстрого онбординга
quick_onboarding_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="✅ Заполню позже"), KeyboardButton(text="📝 Заполнить сейчас")]
    ], resize_keyboard=True
)

# Клавиатура подтверждения действий
confirm_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="✅ Да"), KeyboardButton(text="❌ Нет")]
    ], resize_keyboard=True
)

# Клавиатура навигации
navigation_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="🔙 Назад"), KeyboardButton(text="🏠 Главное меню")]
    ], resize_keyboard=True
)

# Клавиатура редактирования профиля
edit_profile_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="📏 Рост и вес"), KeyboardButton(text="🍽️ Фаза питания")],
        [KeyboardButton(text="📅 Дата операции"), KeyboardButton(text="🚫 Ограничения")],
        [KeyboardButton(text="🔙 Назад к профилю")]
    ], resize_keyboard=True
)