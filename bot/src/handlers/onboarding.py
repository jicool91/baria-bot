# bot/src/handlers/onboarding.py
from datetime import datetime
from aiogram.types import Message
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from ..keyboards import main_patient_kb
from ..db import update_user_onboarding, complete_onboarding, verify_doctor_code

# Глобальная переменная для pool (будет установлена в main.py)
pool = None

class QuickRegStates(StatesGroup):
    # Упрощенная регистрация - только 3 шага
    waiting_basic_info = State()      # Имя + дата операции
    waiting_doctor_code = State()     # Код врача
    waiting_consent = State()         # Согласие

async def start_quick_onboarding(message: Message, state: FSMContext):
    """Начать быстрый онбординг"""
    await message.answer(
        "👋 **Добро пожаловать в бариатрический бот!**\n\n"
        "Я помогу вам в восстановлении после операции.\n\n"
        "🚀 **Быстрый старт** (займет 2 минуты):\n"
        "• Основная информация\n"
        "• Код врача\n"
        "• Согласие на обработку данных\n\n"
        "Остальные данные можно заполнить позже в профиле.\n\n"
        "**Шаг 1 из 3:** Основная информация\n\n"
        "Введите через запятую:\n"
        "• Ваше имя\n"
        "• Дата операции (ДД.ММ.ГГГГ)\n\n"
        "**Пример:** Анна Петрова, 15.06.2024"
    )
    await state.set_state(QuickRegStates.waiting_basic_info)

async def process_basic_info(message: Message, state: FSMContext, db_pool):
    """Обработка основной информации"""
    global pool
    if pool is None:
        pool = db_pool

    try:
        # Парсим ввод пользователя
        parts = [part.strip() for part in message.text.split(',')]

        if len(parts) != 2:
            raise ValueError("Неверный формат")

        full_name = parts[0]
        date_str = parts[1]

        # Валидируем имя
        if len(full_name) < 2:
            raise ValueError("Слишком короткое имя")

        # Парсим дату
        surgery_date = datetime.strptime(date_str, "%d.%m.%Y").date()

        # Проверяем что дата не в будущем
        if surgery_date > datetime.now().date():
            raise ValueError("Дата операции не может быть в будущем")

        # Сохраняем данные
        await update_user_onboarding(
            pool,
            message.from_user.id,
            full_name=full_name,
            surgery_date=surgery_date
        )

        await message.answer(
            f"✅ **Данные сохранены!**\n\n"
            f"👤 Имя: {full_name}\n"
            f"📅 Дата операции: {surgery_date.strftime('%d.%m.%Y')}\n\n"
            f"**Шаг 2 из 3:** Код врача\n\n"
            f"Введите код, который выдал ваш хирург:"
        )
        await state.set_state(QuickRegStates.waiting_doctor_code)

    except ValueError as e:
        if "time data" in str(e):
            error_msg = "❌ Неверный формат даты. Используйте ДД.ММ.ГГГГ"
        elif "будущем" in str(e):
            error_msg = "❌ Дата операции не может быть в будущем"
        else:
            error_msg = "❌ Неверный формат. Используйте: Имя Фамилия, ДД.ММ.ГГГГ"

        await message.answer(
            f"{error_msg}\n\n"
            f"**Пример:** Анна Петрова, 15.06.2024"
        )

async def process_doctor_code_quick(message: Message, state: FSMContext, db_pool):
    """Обработка кода врача в быстром онбординге"""
    global pool
    if pool is None:
        pool = db_pool

    code = message.text.upper().strip()
    doctor = await verify_doctor_code(pool, code)

    if doctor:
        await update_user_onboarding(pool, message.from_user.id, doctor_code=code)
        await message.answer(
            f"✅ **Код принят!**\n\n"
            f"👨‍⚕️ Ваш врач: {doctor['doctor_name']}\n\n"
            f"**Шаг 3 из 3:** Согласие\n\n"
            f"📋 **Согласие на обработку данных**\n\n"
            f"Я согласен(на) на обработку персональных данных в медицинских целях "
            f"и понимаю, что:\n"
            f"• Бот предоставляет справочную информацию\n"
            f"• При ухудшении состояния нужно обращаться к врачу\n"
            f"• Критические симптомы требуют неотложной помощи\n\n"
            f"Напишите **'согласен'** для завершения:"
        )
        await state.set_state(QuickRegStates.waiting_consent)
    else:
        await message.answer(
            "❌ **Неверный код врача**\n\n"
            "Проверьте код у своего хирурга и попробуйте еще раз:"
        )

async def process_consent_quick(message: Message, state: FSMContext, db_pool):
    """Обработка согласия в быстром онбординге"""
    global pool
    if pool is None:
        pool = db_pool

    if message.text.lower() in ['согласен', 'согласна', 'да']:
        await update_user_onboarding(pool, message.from_user.id, consent_given=True)
        await complete_onboarding(pool, message.from_user.id)

        await message.answer(
            "🎉 **Регистрация завершена!**\n\n"
            "Добро пожаловать в систему поддержки после бариатрической операции!\n\n"
            "🚀 **Что доступно прямо сейчас:**\n"
            "🤖 Задавать вопросы ИИ-ассистенту\n"
            "📝 Вести журнал самочувствия\n"
            "👤 Просматривать и редактировать профиль\n\n"
            "💡 **Рекомендуем:**\n"
            "• Заполнить профиль полностью (рост, вес, фаза)\n"
            "• Задать первый вопрос о питании\n"
            "• Начать вести дневник\n\n"
            "Используйте кнопки меню для навигации ⬇️",
            reply_markup=main_patient_kb
        )
        await state.clear()
    else:
        await message.answer(
            "❌ **Без согласия продолжение невозможно**\n\n"
            "Напишите **'согласен'** для завершения регистрации:"
        )
