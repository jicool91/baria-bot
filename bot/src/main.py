import asyncio
import logging
from aiogram import Bot, Dispatcher, F
from aiogram.types import Message
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext

from .settings import settings
from .keyboards import main_patient_kb, main_doctor_kb
from .states import RegStates, JournalStates, AskAIStates
from .handlers import onboarding, profile, ai_assistant, journal
from . import db

logging.basicConfig(level=logging.INFO)

# Глобальный пул соединений
pool = None

async def on_startup():
    global pool
    pool = await db.get_pool()
    
    # Передаем pool в handlers ПОСЛЕ инициализации
    onboarding.pool = pool
    profile.pool = pool
    ai_assistant.pool = pool
    journal.pool = pool
    
    logging.info("🚀 Telegram бот запущен")

async def on_shutdown():
    global pool
    if pool:
        await pool.close()
    logging.info("🛑 Telegram бот остановлен")

async def cmd_start(message: Message, state: FSMContext):
    """Обработчик команды /start"""
    await state.clear()  # Очищаем состояние при старте
    
    user = await db.upsert_user(
        pool,
        message.from_user.id,
        message.from_user.username,
        message.from_user.full_name
    )

    if user['role'] == 'doctor':
        await message.answer(
            "👨‍⚕️ Добро пожаловать, доктор! Функции для врачей в разработке.",
            reply_markup=main_doctor_kb
        )
        return

    # Проверяем завершен ли онбординг
    if user['onboarding_completed']:
        await message.answer(
            "🎉 С возвращением! Как дела?",
            reply_markup=main_patient_kb
        )
    else:
        # Начинаем быстрый онбординг
        await onboarding.start_quick_onboarding(message, state)

async def handle_patient_menu(message: Message, state: FSMContext):
    """Обработка главного меню пациента"""
    await state.clear()  # Очищаем состояние при переходе в меню
    text = message.text

    if text == "👤 Профиль":
        await profile.show_profile(message, pool)

    elif text == "🤖 Вопрос ИИ":
        await ai_assistant.start_ai_session(message, state)

    elif text == "📝 Журнал":
        await journal.start_journal_entry(message, state)

    elif text == "📅 План питания":
        await message.answer("📅 Планы питания находятся в разработке. Скоро будет доступно!")

    elif text == "💊 Витамины":
        await message.answer("💊 Модуль витаминов находится в разработке. Скоро будет доступно!")

    elif text == "🩺 Связь с врачом":
        await message.answer("🩺 Связь с врачом находится в разработке. Скоро будет доступно!")

    else:
        await message.answer(f"Функция '{text}' находится в разработке. Скоро будет доступна!")

async def handle_profile_commands(message: Message):
    """Обработка команд профиля"""
    text = message.text.lower().strip()

    # Команда редактирования профиля
    if text == "редактировать профиль":
        await profile.handle_profile_edit(message)
        return True

    # Команды быстрого обновления профиля
    return await profile.handle_simple_profile_updates(message, pool)

async def handle_other_messages(message: Message, state: FSMContext):
    """Обработчик прочих сообщений"""
    # Проверяем команды профиля
    if await handle_profile_commands(message):
        return

    # Стандартный ответ
    await message.answer(
        "Используйте кнопки меню для навигации или команду /start для начала работы.",
        reply_markup=main_patient_kb
    )

# Обработчики с передачей pool
async def process_basic_info_wrapper(message: Message, state: FSMContext):
    return await onboarding.process_basic_info(message, state, pool)

async def process_doctor_code_wrapper(message: Message, state: FSMContext):
    return await onboarding.process_doctor_code_quick(message, state, pool)

async def process_consent_wrapper(message: Message, state: FSMContext):
    return await onboarding.process_consent_quick(message, state, pool)

async def process_field_choice_wrapper(message: Message, state: FSMContext):
    return await profile.process_field_choice(message, state, pool)

async def update_height_weight_wrapper(message: Message, state: FSMContext):
    return await profile.update_height_weight(message, state, pool)

async def update_phase_wrapper(message: Message, state: FSMContext):
    return await profile.update_phase(message, state, pool)

async def update_restrictions_wrapper(message: Message, state: FSMContext):
    return await profile.update_restrictions(message, state, pool)

async def handle_ai_question_wrapper(message: Message, state: FSMContext):
    return await ai_assistant.handle_ai_question(message, state, pool)

async def handle_journal_entry_wrapper(message: Message, state: FSMContext):
    return await journal.handle_journal_entry(message, state, pool)

async def main():
    bot = Bot(settings.BOT_TOKEN)
    dp = Dispatcher()

    # Регистрируем startup/shutdown
    dp.startup.register(on_startup)
    dp.shutdown.register(on_shutdown)

    # Обработчики команд
    dp.message.register(cmd_start, CommandStart())

    # Обработчики меню (ВЫСОКИЙ ПРИОРИТЕТ - регистрируем ПЕРВЫМИ)
    dp.message.register(handle_patient_menu, F.text.in_({
        "📅 План питания", "💊 Витамины", "📝 Журнал",
        "🤖 Вопрос ИИ", "🩺 Связь с врачом", "👤 Профиль"
    }))

    # Обработчики FSM состояний (СРЕДНИЙ ПРИОРИТЕТ)
    # Онбординг
    dp.message.register(process_basic_info_wrapper, onboarding.QuickRegStates.waiting_basic_info)
    dp.message.register(process_doctor_code_wrapper, onboarding.QuickRegStates.waiting_doctor_code)
    dp.message.register(process_consent_wrapper, onboarding.QuickRegStates.waiting_consent)

    # Профиль
    dp.message.register(process_field_choice_wrapper, profile.ProfileStates.choosing_field)
    dp.message.register(update_height_weight_wrapper, profile.ProfileStates.editing_height_weight)
    dp.message.register(update_phase_wrapper, profile.ProfileStates.editing_phase)
    dp.message.register(update_restrictions_wrapper, profile.ProfileStates.editing_restrictions)

    # ИИ
    dp.message.register(handle_ai_question_wrapper, AskAIStates.waiting_question)

    # Журнал
    dp.message.register(handle_journal_entry_wrapper, JournalStates.waiting_journal_data)

    # Обработчик остальных сообщений (НИЗКИЙ ПРИОРИТЕТ - последний)
    dp.message.register(handle_other_messages, F.text)

    logging.info("🚀 Запуск Telegram бота...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
