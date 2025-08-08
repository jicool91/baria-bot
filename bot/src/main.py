import asyncio
import logging
from aiogram import Bot, Dispatcher, F
from aiogram.types import Message
from aiogram.filters import CommandStart
from .settings import settings
from .keyboards import main_patient_kb

logging.basicConfig(level=logging.INFO)

async def cmd_start(message: Message):
    await message.answer("🎉 Бот работает! Привет из бариатрического бота!", reply_markup=main_patient_kb)

async def handle_text(message: Message):
    text = message.text
    if text == "🤖 Вопрос ИИ":
        await message.answer("Тестовый ответ от ИИ. RAG-сервис скоро будет подключен!")
    elif text in ["📅 План питания", "💊 Витамины", "🚰 Вода", "📝 Журнал", "🩺 Связь с врачом"]:
        await message.answer(f"Функция '{text}' в разработке.")
    else:
        await message.answer(f"Получил сообщение: {text}")

async def main():
    bot = Bot(settings.BOT_TOKEN)
    dp = Dispatcher()
    
    dp.message.register(cmd_start, CommandStart())
    dp.message.register(handle_text, F.text)
    
    logging.info("🚀 Starting bot...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
