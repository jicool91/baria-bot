import asyncio
import logging
from aiogram import Bot, Dispatcher, F
from aiogram.types import Message
from aiogram.filters import CommandStart
from .settings import settings
from .keyboards import main_patient_kb
import httpx

logging.basicConfig(level=logging.INFO)

# Простое хранилище для отслеживания состояния ожидания AI-ответов
awaiting_ai_users = set()

async def cmd_start(message: Message):
    await message.answer(
        "🎉 Добро пожаловать в бариатрический бот!\n\n"
        "Я помогу с сопровождением после бариатрической операции.",
        reply_markup=main_patient_kb
    )

async def handle_patient_menu(message: Message):
    text = message.text
    if text == "🤖 Вопрос ИИ":
        await message.answer("Задайте ваш вопрос. Я сверюсь с медицинскими протоколами и отвечу.")
        awaiting_ai_users.add(message.from_user.id)
    elif text == "📝 Журнал":
        await message.answer("Отправьте данные одной строкой: вес(кг);настроение(1-5);симптомы\nПример: 85.2;4;тошнота нет")
    else:
        await message.answer(f"Функция '{text}' находится в разработке. Скоро будет доступна!")

async def handle_ai_question(message: Message):
    if message.from_user.id in awaiting_ai_users:
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

        awaiting_ai_users.discard(message.from_user.id)
    else:
        await message.answer(f"Получил сообщение: {message.text}\n\nИспользуйте кнопки меню для навигации.")

async def main():
    bot = Bot(settings.BOT_TOKEN)
    dp = Dispatcher()

    # Регистрируем обработчики
    dp.message.register(cmd_start, CommandStart())
    dp.message.register(handle_patient_menu, F.text.in_({
        "📅 План питания", "💊 Витамины", "🚰 Вода", "📝 Журнал", "🤖 Вопрос ИИ", "🩺 Связь с врачом"
    }))
    dp.message.register(handle_ai_question, F.text)

    logging.info("🚀 Запуск Telegram бота...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())