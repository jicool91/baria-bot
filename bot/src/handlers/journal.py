# bot/src/handlers/journal.py
import logging
from datetime import datetime
from aiogram.types import Message
from aiogram.fsm.context import FSMContext
from ..red_flags import RedFlagDetector
from ..db import get_user_profile, save_red_flag, save_journal_entry

# Глобальная переменная для pool (будет установлена в main.py)
pool = None

async def start_journal_entry(message: Message, state: FSMContext):
    """Начать ввод записи в журнал"""
    await message.answer(
        "📝 **Журнал самочувствия**\n\n"
        "Ведение дневника поможет вам и врачу отслеживать динамику восстановления.\n\n"
        "📊 **Формат записи:**\n"
        "`вес,настроение,симптомы`\n\n"
        "📋 **Примеры:**\n"
        "• `78.5,4,легкая тошнота утром`\n"
        "• `75,хорошо,все в порядке`\n"
        "• `80.2,плохое,головокружение`\n\n"
        "📈 **Настроение:**\n"
        "Число от 1 до 5 или словами:\n"
        "1 (очень плохо) → 2 (плохо) → 3 (нормально) → 4 (хорошо) → 5 (отлично)\n\n"
        "💡 **Совет:** Симптомы можно не указывать\n\n"
        "Введите данные:"
    )
    from ..states import JournalStates
    await state.set_state(JournalStates.waiting_journal_data)

async def handle_journal_entry(message: Message, state: FSMContext, db_pool):
    """Обработка записи в журнале"""
    global pool
    if pool is None:
        pool = db_pool

    try:
        # Парсим данные
        parts = [part.strip() for part in message.text.split(',')]
        if len(parts) < 2:
            raise ValueError("Недостаточно данных")

        weight = float(parts[0])
        
        # Пытаемся преобразовать настроение в число
        mood_text = parts[1].lower()
        if mood_text.isdigit():
            mood = int(mood_text)
        else:
            # Пытаемся распознать текстовое описание настроения
            mood_map = {
                'очень плохо': 1, 'очень плохое': 1, 'ужасно': 1, 'плохо': 2, 'плохое': 2,
                'нормально': 3, 'средне': 3, 'неплохо': 3, 'хорошо': 4, 'хорошее': 4,
                'отлично': 5, 'отличное': 5, 'прекрасно': 5, 'замечательно': 5
            }
            mood = mood_map.get(mood_text, None)
            if mood is None:
                raise ValueError(f"Не могу распознать настроение '{parts[1]}'. Используйте число от 1 до 5 или слова: плохо, нормально, хорошо, отлично")
        
        symptoms = parts[2] if len(parts) > 2 else ""

        # Валидация
        if not (1 <= mood <= 5):
            raise ValueError("Настроение должно быть от 1 до 5")

        if weight < 30 or weight > 300:
            raise ValueError("Проверьте корректность веса")

        # Проверяем симптомы на red-flags
        if symptoms:
            is_critical, detected_flags, severity = RedFlagDetector.check_symptoms(symptoms)

            if is_critical:
                user = await get_user_profile(pool, message.from_user.id)
                if user:
                    await save_red_flag(pool, user['id'], symptoms, severity)

                warning = RedFlagDetector.format_warning(detected_flags)
                await message.answer(warning)
                await state.clear()
                return

        # Сохраняем запись
        entry = await save_journal_entry(pool, message.from_user.id, weight, mood, symptoms)

        if entry:
            # Эмодзи для настроения
            mood_emoji = ["😰", "😟", "😐", "😊", "🤩"][mood - 1]
            
            await message.answer(
                f"✅ **Запись сохранена!**\n\n"
                f"📅 **Дата:** {entry['entry_date'].strftime('%d.%m.%Y')}\n"
                f"⚖️ **Вес:** {weight} кг\n"
                f"😊 **Настроение:** {mood}/5 {mood_emoji}\n"
                f"🩺 **Симптомы:** {symptoms if symptoms else 'Не указаны'}\n\n"
                f"📊 Данные переданы вашему врачу для мониторинга.\n"
                f"📝 Продолжайте вести дневник ежедневно для лучших результатов!"
            )
        else:
            await message.answer("❌ Ошибка сохранения записи. Попробуйте еще раз.")

    except ValueError as e:
        error_msg = str(e)
        if "could not convert" in error_msg or "invalid literal" in error_msg:
            await message.answer(
                "❌ **Ошибка формата данных**\n\n"
                "Правильный формат: `вес,настроение,симптомы`\n\n"
                "📋 **Примеры:**\n"
                "• `75.2,4,все хорошо`\n"
                "• `78,хорошо,небольшая тошнота`\n"
                "• `76.5,отлично,отличное самочувствие`\n"
                "• `80,плохое` (без симптомов)\n\n"
                "**Настроение:** число от 1 до 5 или словами (плохо, хорошо, отлично)\n"
                "**Вес:** в килограммах (можно с десятыми)"
            )
        else:
            await message.answer(f"❌ {error_msg}")

    except Exception as e:
        logging.error(f"Journal entry error: {e}")
        await message.answer(
            "❌ Произошла ошибка при сохранении.\n"
            "Попробуйте еще раз или обратитесь в поддержку."
        )

    await state.clear()
