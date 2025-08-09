# bot/src/handlers/ai_assistant.py
import logging
import httpx
from aiogram.types import Message
from aiogram.fsm.context import FSMContext
from ..settings import settings
from ..red_flags import RedFlagDetector
from ..db import get_user_profile, save_red_flag

# Глобальная переменная для pool (будет установлена в main.py)
pool = None

async def start_ai_session(message: Message, state: FSMContext):
    """Начать сессию с ИИ-ассистентом"""
    await message.answer(
        "🤖 **Медицинский ИИ-ассистент**\n\n"
        "Задайте ваш вопрос о:\n"
        "• Питании по фазам восстановления\n"
        "• Самочувствии и симптомах\n"
        "• Витаминах и добавках\n"
        "• Физической активности\n\n"
        "⚠️ **Важно:** При критических симптомах немедленно обращайтесь к врачу!\n\n"
        "Введите ваш вопрос:"
    )
    from ..states import AskAIStates
    await state.set_state(AskAIStates.waiting_question)

async def handle_ai_question(message: Message, state: FSMContext, db_pool):
    """Обработка вопроса к ИИ"""
    global pool
    if pool is None:
        pool = db_pool

    # Проверяем на red-flags
    is_critical, detected_flags, severity = RedFlagDetector.check_symptoms(message.text)

    if is_critical:
        # Сохраняем критический симптом
        user = await get_user_profile(pool, message.from_user.id)
        if user:
            await save_red_flag(pool, user['id'], message.text, severity)

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
                final_answer += f"\n\n📚 **Источники:** {', '.join(sources)}"

            # Добавляем информацию о возможности задать еще вопрос
            final_answer += "\n\n💬 Есть еще вопросы? Просто напишите следующий вопрос или используйте меню."

            await message.answer(final_answer)
        else:
            await message.answer(
                "❌ Сервис ИИ временно недоступен.\n\n"
                "🏥 Рекомендуем обратиться к врачу за консультацией."
            )

    except Exception as e:
        logging.error(f"RAG error: {e}")
        await message.answer(
            "❌ Ошибка при обращении к ИИ.\n\n"
            "🔄 Попробуйте позже или обратитесь к врачу."
        )

    await state.clear()
