from aiogram.types import ReplyKeyboardMarkup, KeyboardButton

main_patient_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="📅 План питания"), KeyboardButton(text="💊 Витамины")],
        [KeyboardButton(text="🚰 Вода"), KeyboardButton(text="📝 Журнал")],
        [KeyboardButton(text="🤖 Вопрос ИИ"), KeyboardButton(text="🩺 Связь с врачом")],
    ], resize_keyboard=True
)

main_doctor_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="👥 Мои пациенты"), KeyboardButton(text="🗓 Записи пациентов")],
        [KeyboardButton(text="📨 Входящие вопросы")],
    ], resize_keyboard=True
)
