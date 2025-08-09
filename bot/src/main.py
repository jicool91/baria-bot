import asyncio
import logging
from datetime import datetime
import re
from aiogram import Bot, Dispatcher, F
from aiogram.types import Message
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext

from .settings import settings
from .keyboards import main_patient_kb
from .states import RegStates, JournalStates, AskAIStates
from .red_flags import RedFlagDetector
from . import db
import httpx

logging.basicConfig(level=logging.INFO)

# Глобальный пул соединений
pool = None

async def on_startup():
    global pool
    pool = await db.get_pool()
    logging.info("🚀 Telegram бот запущен")

async def on_shutdown():
    global pool
    if pool:
        await pool.close()
    logging.info("🛑 Telegram бот остановлен")

async def cmd_start(message: Message, state: FSMContext):
    """Обработчик команды /start"""
    user = await db.upsert_user(
        pool,
        message.from_user.id,
        message.from_user.username,
        message.from_user.full_name
    )

    if user['role'] == 'doctor':
        await message.answer("👨‍⚕️ Добро пожаловать, доктор! Функции для врачей в разработке.")
        return

    # Проверяем завершен ли онбординг
    if user['onboarding_completed']:
        await message.answer(
            "🎉 С возвращением! Как дела?",
            reply_markup=main_patient_kb
        )
    else:
        # Начинаем онбординг
        await message.answer(
            "👋 Добро пожаловать в бариатрический бот!\n\n"
            "Я помогу вам в восстановлении после операции. "
            "Для начала мне нужна информация о вас.\n\n"
            "Как вас зовут? (Полное имя)"
        )
        await state.set_state(RegStates.waiting_full_name)

async def process_full_name(message: Message, state: FSMContext):
    """Обработка ввода полного имени"""
    await db.update_user_onboarding(pool, message.from_user.id, full_name=message.text)

    await message.answer(
        f"Приятно познакомиться, {message.text}!\n\n"
        "Когда была проведена бариатрическая операция?\n"
        "Укажите дату в формате ДД.ММ.ГГГГ (например: 15.06.2024)"
    )
    await state.set_state(RegStates.waiting_surgery_date)

async def process_surgery_date(message: Message, state: FSMContext):
    """Обработка даты операции"""
    try:
        # Парсим дату
        date_obj = datetime.strptime(message.text, "%d.%m.%Y").date()
        await db.update_user_onboarding(pool, message.from_user.id, surgery_date=date_obj)

        await message.answer(
            "✅ Дата операции сохранена.\n\n"
            "Введите код вашего врача (получили от вашего хирурга):"
        )
        await state.set_state(RegStates.waiting_doctor_code)

    except ValueError:
        await message.answer(
            "❌ Неверный формат даты. Пожалуйста, используйте формат ДД.ММ.ГГГГ\n"
            "Например: 15.06.2024"
        )

async def process_doctor_code(message: Message, state: FSMContext):
    """Обработка кода врача"""
    code = message.text.upper().strip()
    doctor = await db.verify_doctor_code(pool, code)

    if doctor:
        await db.update_user_onboarding(pool, message.from_user.id, doctor_code=code)
        await message.answer(
            f"✅ Код принят! Ваш врач: {doctor['doctor_name']}\n\n"
            "Укажите ваш рост и текущий вес.\n"
            "Формат: рост,вес (например: 170,85.5)"
        )
        await state.set_state(RegStates.waiting_height_weight)
    else:
        await message.answer(
            "❌ Неверный код врача. Проверьте код у своего хирурга.\n"
            "Введите код еще раз:"
        )

async def process_height_weight(message: Message, state: FSMContext):
    """Обработка роста и веса"""
    try:
        parts = message.text.replace(' ', '').split(',')
        if len(parts) != 2:
            raise ValueError("Неверный формат")

        height = int(parts[0])
        weight = float(parts[1])

        if height < 100 or height > 250:
            raise ValueError("Неверный рост")
        if weight < 30 or weight > 300:
            raise ValueError("Неверный вес")

        await db.update_user_onboarding(
            pool,
            message.from_user.id,
            height_cm=height,
            weight_kg=weight
        )

        await message.answer(
            f"✅ Рост: {height} см, Вес: {weight} кг\n\n"
            "На какой фазе питания вы сейчас находитесь?\n\n"
            "1️⃣ Жидкая (первые 1-2 недели)\n"
            "2️⃣ Пюре (2-4 недели)\n"
            "3️⃣ Мягкая (4-8 недель)\n"
            "4️⃣ Обычная (после 8 недель)\n\n"
            "Введите номер фазы (1-4):"
        )
        await state.set_state(RegStates.waiting_phase)

    except ValueError:
        await message.answer(
            "❌ Неверный формат. Используйте: рост,вес\n"
            "Например: 170,85.5"
        )

async def process_phase(message: Message, state: FSMContext):
    """Обработка фазы питания"""
    phases = {
        "1": "жидкая",
        "2": "пюре",
        "3": "мягкая",
        "4": "обычная"
    }

    phase_num = message.text.strip()
    if phase_num in phases:
        phase = phases[phase_num]
        await db.update_user_onboarding(pool, message.from_user.id, current_phase=phase)

        await message.answer(
            f"✅ Фаза питания: {phase}\n\n"
            "Есть ли у вас пищевые ограничения или аллергии?\n"
            "(Если нет, напишите 'нет')"
        )
        await state.set_state(RegStates.waiting_restrictions)
    else:
        await message.answer("❌ Введите номер от 1 до 4")

async def process_restrictions(message: Message, state: FSMContext):
    """Обработка ограничений"""
    restrictions = message.text if message.text.lower() != 'нет' else None
    await db.update_user_onboarding(pool, message.from_user.id, dietary_restrictions=restrictions)

    await message.answer(
        "📋 Согласие на обработку данных\n\n"
        "Я согласен(на) на обработку персональных данных в медицинских целях "
        "и понимаю, что:\n"
        "• Бот предоставляет справочную информацию\n"
        "• При ухудшении состояния нужно обращаться к врачу\n"
        "• Критические симптомы требуют неотложной помощи\n\n"
        "Напишите 'согласен' для продолжения:"
    )
    await state.set_state(RegStates.waiting_consent)

async def process_consent(message: Message, state: FSMContext):
    """Обработка согласия"""
    if message.text.lower() in ['согласен', 'согласна', 'да']:
        await db.update_user_onboarding(pool, message.from_user.id, consent_given=True)
        await db.complete_onboarding(pool, message.from_user.id)

        await message.answer(
            "🎉 Регистрация завершена!\n\n"
            "Теперь вы можете:\n"
            "🤖 Задавать вопросы ИИ-ассистенту\n"
            "📝 Вести журнал веса и самочувствия\n"
            "📅 Получать планы питания\n"
            "👤 Просматривать и редактировать профиль\n"
            "🚨 Получать помощь при критических симптомах\n\n"
            "Используйте кнопки меню для навигации.",
            reply_markup=main_patient_kb
        )
        await state.clear()
    else:
        await message.answer(
            "❌ Без согласия на обработку данных продолжение невозможно.\n"
            "Напишите 'согласен' для продолжения:"
        )

async def show_profile(message: Message):
    """Показать профиль пользователя"""
    user = await db.get_user_profile(pool, message.from_user.id)

    if not user:
        await message.answer("❌ Профиль не найден")
        return

    # Форматируем дату операции
    surgery_date = user.get('surgery_date')
    surgery_str = surgery_date.strftime('%d.%m.%Y') if surgery_date else '❌ Не указана'

    # Считаем заполненность профиля
    completed_fields = 0
    total_fields = 6

    fields_status = {
        'full_name': user.get('full_name'),
        'surgery_date': user.get('surgery_date'),
        'height_cm': user.get('height_cm'),
        'weight_kg': user.get('weight_kg'),
        'current_phase': user.get('current_phase'),
        'doctor_code': user.get('doctor_code')
    }

    for field, value in fields_status.items():
        if value:
            completed_fields += 1

    completeness = int((completed_fields / total_fields) * 100)

    profile_text = f"""
👤 **Ваш профиль** ({completeness}% заполнен)

📝 **Личные данные:**
• Имя: {user.get('full_name', '❌ Не указано')}
• Дата операции: {surgery_str}
• Код врача: {user.get('doctor_code', '❌ Не указан')}

📏 **Физические параметры:**
• Рост: {user.get('height_cm', '❌ Не указан')} {'см' if user.get('height_cm') else ''}
• Вес: {user.get('weight_kg', '❌ Не указан')} {'кг' if user.get('weight_kg') else ''}

🍽️ **Питание:**
• Текущая фаза: {user.get('current_phase', '❌ Не указана')}
• Ограничения: {user.get('dietary_restrictions', 'Не указаны')}

📊 **Статистика:**
• Дата регистрации: {user.get('created_at').strftime('%d.%m.%Y')}
• Онбординг: {'✅ Завершен' if user.get('onboarding_completed') else '⏳ В процессе'}

💡 **Для редактирования напишите:** редактировать профиль
"""

    await message.answer(profile_text)

async def handle_profile_edit(message: Message):
    """Инструкции по редактированию профиля"""
    await message.answer(
        "✏️ **Редактирование профиля**\n\n"
        "Доступные команды для быстрого обновления:\n\n"
        "📅 `дата операции: ДД.ММ.ГГГГ`\n"
        "Пример: дата операции: 15.06.2024\n\n"
        "📏 `рост вес: рост,вес`\n"
        "Пример: рост вес: 170,75.5\n\n"
        "🍽️ `фаза: номер`\n"
        "Пример: фаза: 3\n"
        "(1-жидкая, 2-пюре, 3-мягкая, 4-обычная)\n\n"
        "🚫 `ограничения: текст`\n"
        "Пример: ограничения: непереносимость лактозы\n\n"
        "Просто напишите команду в чат!"
    )

async def handle_simple_profile_updates(message: Message):
    """Обработка команд редактирования профиля"""
    text = message.text.lower().strip()

    try:
        # Обновление даты операции
        if text.startswith('дата операции:'):
            date_str = text.split(':', 1)[1].strip()
            surgery_date = datetime.strptime(date_str, "%d.%m.%Y").date()
            await db.update_user_onboarding(pool, message.from_user.id, surgery_date=surgery_date)
            await message.answer(f"✅ Дата операции обновлена: {surgery_date.strftime('%d.%m.%Y')}")
            return True

        # Обновление роста и веса
        elif text.startswith('рост вес:'):
            values = text.split(':', 1)[1].strip()
            parts = values.replace(' ', '').split(',')
            if len(parts) == 2:
                height = int(parts[0])
                weight = float(parts[1])
                if 100 <= height <= 250 and 30 <= weight <= 300:
                    await db.update_user_onboarding(
                        pool, message.from_user.id,
                        height_cm=height, weight_kg=weight
                    )
                    await message.answer(f"✅ Обновлено: рост {height} см, вес {weight} кг")
                    return True
                else:
                    raise ValueError("Неверные значения")
            else:
                raise ValueError("Неверный формат")

        # Обновление фазы
        elif text.startswith('фаза:'):
            phase_num = text.split(':', 1)[1].strip()
            phases = {"1": "жидкая", "2": "пюре", "3": "мягкая", "4": "обычная"}
            if phase_num in phases:
                phase = phases[phase_num]
                await db.update_user_onboarding(pool, message.from_user.id, current_phase=phase)
                await message.answer(f"✅ Фаза питания обновлена: {phase}")
                return True
            else:
                raise ValueError("Неверная фаза")

        # Обновление ограничений
        elif text.startswith('ограничения:'):
            restrictions = text.split(':', 1)[1].strip()
            restrictions = restrictions if restrictions.lower() != 'нет' else None
            await db.update_user_onboarding(pool, message.from_user.id, dietary_restrictions=restrictions)
            restrictions_text = restrictions if restrictions else "отсутствуют"
            await message.answer(f"✅ Ограничения обновлены: {restrictions_text}")
            return True

        return False

    except (ValueError, IndexError) as e:
        await message.answer(
            "❌ Ошибка в формате команды.\n\n"
            "Правильные форматы:\n"
            "• `дата операции: ДД.ММ.ГГГГ`\n"
            "• `рост вес: 170,75.5`\n"
            "• `фаза: 1-4`\n"
            "• `ограничения: текст или нет`"
        )
        return True

async def handle_patient_menu(message: Message, state: FSMContext):
    """Обработка меню пациента"""
    text = message.text

    if text == "👤 Профиль":
        await show_profile(message)

    elif text == "🤖 Вопрос ИИ":
        await message.answer(
            "🤖 Задайте ваш вопрос о питании, самочувствии или восстановлении.\n"
            "Я отвечу на основе медицинских протоколов.\n\n"
            "⚠️ При критических симптомах немедленно обращайтесь к врачу!"
        )
        await state.set_state(AskAIStates.waiting_question)

    elif text == "📝 Журнал":
        await message.answer(
            "📝 Ведение журнала самочувствия\n\n"
            "Отправьте данные в формате:\n"
            "вес,настроение,симптомы\n\n"
            "Например: 78.5,4,тошнота легкая\n"
            "Настроение от 1 (плохо) до 5 (отлично)"
        )
        await state.set_state(JournalStates.waiting_journal_data)

    else:
        await message.answer(f"Функция '{text}' находится в разработке. Скоро будет доступна!")

async def handle_ai_question(message: Message, state: FSMContext):
    """Обработка вопроса к ИИ"""
    # Проверяем на red-flags
    is_critical, detected_flags, severity = RedFlagDetector.check_symptoms(message.text)

    if is_critical:
        # Сохраняем критический симптом
        user = await db.get_user_profile(pool, message.from_user.id)
        if user:
            await db.save_red_flag(pool, user['id'], message.text, severity)

        # Отправляем предупреждение
        warning = RedFlagDetector.format_warning(detected_flags)
        await message.answer(warning)
        await state.clear()
        return

    # Обычный запрос к RAG
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(settings.RAG_URL, json={
                "user_id": message.from_user.id,
                "question": message.text
            })

        if response.status_code == 200:
            data = response.json()
            answer = data.get("answer", "Нет ответа")
            sources = data.get("sources", [])

            final_answer = answer
            if sources:
                final_answer += f"\n\n📚 Источники: {', '.join(sources)}"

            await message.answer(final_answer)
        else:
            await message.answer("❌ Сервис ИИ временно недоступен. Попробуйте позже или обратитесь к врачу.")

    except Exception as e:
        logging.error(f"RAG error: {e}")
        await message.answer("❌ Ошибка при обращении к ИИ. Попробуйте позже.")

    await state.clear()

async def handle_journal_entry(message: Message, state: FSMContext):
    """Обработка записи в журнале"""
    try:
        parts = message.text.split(',')
        if len(parts) < 3:
            raise ValueError("Недостаточно данных")

        weight = float(parts[0].strip())
        mood = int(parts[1].strip())
        symptoms = parts[2].strip()

        if not (1 <= mood <= 5):
            raise ValueError("Настроение должно быть от 1 до 5")

        # Проверяем симптомы на red-flags
        is_critical, detected_flags, severity = RedFlagDetector.check_symptoms(symptoms)

        if is_critical:
            user = await db.get_user_profile(pool, message.from_user.id)
            if user:
                await db.save_red_flag(pool, user['id'], symptoms, severity)

            warning = RedFlagDetector.format_warning(detected_flags)
            await message.answer(warning)
            await state.clear()
            return

        # Сохраняем запись
        entry = await db.save_journal_entry(pool, message.from_user.id, weight, mood, symptoms)

        if entry:
            await message.answer(
                f"✅ Запись сохранена!\n\n"
                f"📅 Дата: {entry['entry_date']}\n"
                f"⚖️ Вес: {weight} кг\n"
                f"😊 Настроение: {mood}/5\n"
                f"🩺 Симптомы: {symptoms}"
            )
        else:
            await message.answer("❌ Ошибка сохранения записи")

    except ValueError as e:
        await message.answer(
            "❌ Неверный формат данных.\n"
            "Используйте: вес,настроение,симптомы\n"
            "Например: 78.5,4,тошнота легкая"
        )
    except Exception as e:
        logging.error(f"Journal entry error: {e}")
        await message.answer("❌ Ошибка при сохранении записи. Попробуйте еще раз.")

    await state.clear()

async def handle_other_messages(message: Message):
    """Обработчик прочих сообщений"""
    text = message.text.lower().strip()

    # Проверяем команды редактирования профиля
    if text == "редактировать профиль":
        await handle_profile_edit(message)
        return

    # Проверяем команды обновления профиля
    if await handle_simple_profile_updates(message):
        return

    # Стандартный ответ
    await message.answer(
        "Используйте кнопки меню для навигации или команду /start для начала работы.",
        reply_markup=main_patient_kb
    )

async def main():
    bot = Bot(settings.BOT_TOKEN)
    dp = Dispatcher()

    # Регистрируем startup/shutdown
    dp.startup.register(on_startup)
    dp.shutdown.register(on_shutdown)

    # Обработчики команд
    dp.message.register(cmd_start, CommandStart())

    # Обработчики FSM состояний
    dp.message.register(process_full_name, RegStates.waiting_full_name)
    dp.message.register(process_surgery_date, RegStates.waiting_surgery_date)
    dp.message.register(process_doctor_code, RegStates.waiting_doctor_code)
    dp.message.register(process_height_weight, RegStates.waiting_height_weight)
    dp.message.register(process_phase, RegStates.waiting_phase)
    dp.message.register(process_restrictions, RegStates.waiting_restrictions)
    dp.message.register(process_consent, RegStates.waiting_consent)

    dp.message.register(handle_ai_question, AskAIStates.waiting_question)
    dp.message.register(handle_journal_entry, JournalStates.waiting_journal_data)

    # Обработчики меню (с добавленной кнопкой профиля)
    dp.message.register(handle_patient_menu, F.text.in_({
        "📅 План питания", "💊 Витамины", "🚰 Вода", "📝 Журнал",
        "🤖 Вопрос ИИ", "🩺 Связь с врачом", "👤 Профиль"
    }))

    # Обработчик остальных сообщений
    dp.message.register(handle_other_messages, F.text)

    logging.info("🚀 Запуск Telegram бота...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())