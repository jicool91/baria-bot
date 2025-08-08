import asyncio
import logging
from aiogram import Bot, Dispatcher, F
from aiogram.types import Message
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext

from .settings import settings
from .keyboards import main_patient_kb, main_doctor_kb
from .states import RegStates
from . import db
import httpx

logging.basicConfig(level=logging.INFO)

async def on_startup():
    """Startup handler without parameters"""
    pass

async def on_shutdown():
    """Shutdown handler without parameters"""
    pass

async def cmd_start(message: Message, state: FSMContext):
    # Простая версия без БД пока что
    await message.answer("Привет! Я помогу с сопровождением после бариатрии.", reply_markup=main_patient_kb)

async def handle_patient_menu(message: Message):
    text = message.text
    if text == "🤖 Вопрос ИИ":
        await message.answer("Сформулируйте вопрос. Я сверюсь с методичками и отвечу.")
        # Простое решение - сохраняем в memory
        handle_patient_menu.awaiting_ai = getattr(handle_patient_menu, 'awaiting_ai', set())
        handle_patient_menu.awaiting_ai.add(message.from_user.id)
    elif text == "📝 Журнал":
        await message.answer("Отправьте вес (кг), настроение (1-5) и симптомы одной строкой: 85.2;4;тошнота нет")
    else:
        await message.answer("Функция в разработке.")

async def handle_any_message(message: Message):
    awaiting_ai = getattr(handle_patient_menu, 'awaiting_ai', set())

    if message.from_user.id in awaiting_ai:
        try:
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.post(settings.RAG_URL, json={
                    "user_id": message.from_user.id,
                    "question": message.text
                })
            if r.status_code == 200:
                await message.answer(r.json().get("answer", "Нет ответа"))
            else:
                await message.answer("Сервис ИИ недоступен. Попробуйте позже.")
        except Exception as e:
            logging.error(f"RAG error: {e}")
            await message.answer("Ошибка при обращении к ИИ.")

        awaiting_ai.discard(message.from_user.id)

async def main():
    bot = Bot(settings.BOT_TOKEN)
    dp = Dispatcher()

    # Простые обработчики без параметров
    dp.startup.register(on_startup)
    dp.shutdown.register(on_shutdown)

    dp.message.register(cmd_start, CommandStart())
    dp.message.register(handle_patient_menu, F.text.in_({
        "📅 План питания", "💊 Витамины", "🚰 Вода", "📝 Журнал", "🤖 Вопрос ИИ", "🩺 Связь с врачом"
    }))
    dp.message.register(handle_any_message, F.text)

    logging.info("Starting bot...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())