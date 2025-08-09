# bot/src/handlers/profile.py
from aiogram.types import Message, ReplyKeyboardMarkup, KeyboardButton
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from ..db import get_user_profile, update_user_onboarding
from ..keyboards import main_patient_kb

class ProfileStates(StatesGroup):
    choosing_field = State()
    editing_height_weight = State()
    editing_phase = State()
    editing_restrictions = State()

# Клавиатура профиля
profile_kb = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="📊 Мои данные"), KeyboardButton(text="✏️ Редактировать")],
        [KeyboardButton(text="📈 Статистика"), KeyboardButton(text="🔙 Главное меню")]
    ], resize_keyboard=True
)

async def show_profile(message: Message, pool):
    """Показать профиль пользователя"""
    profile = await get_user_profile(pool, message.from_user.id)

    if not profile:
        await message.answer("❌ Профиль не найден")
        return

    # Форматируем дату операции
    surgery_date = profile.get('surgery_date')
    surgery_str = surgery_date.strftime('%d.%m.%Y') if surgery_date else 'Не указана'

    # Статус заполненности профиля
    completed_fields = 0
    total_fields = 6

    fields_status = {
        'full_name': profile.get('full_name'),
        'surgery_date': profile.get('surgery_date'),
        'height_cm': profile.get('height_cm'),
        'weight_kg': profile.get('weight_kg'),
        'current_phase': profile.get('current_phase'),
        'doctor_code': profile.get('doctor_code')
    }

    for field, value in fields_status.items():
        if value:
            completed_fields += 1

    completeness = int((completed_fields / total_fields) * 100)

    profile_text = f"""
👤 **Ваш профиль** ({completeness}% заполнен)

📝 **Личные данные:**
• Имя: {profile.get('full_name', '❌ Не указано')}
• Дата операции: {surgery_str}
• Код врача: {profile.get('doctor_code', '❌ Не указан')}

📏 **Физические параметры:**
• Рост: {profile.get('height_cm', '❌ Не указан')} {'см' if profile.get('height_cm') else ''}
• Вес: {profile.get('weight_kg', '❌ Не указан')} {'кг' if profile.get('weight_kg') else ''}

🍽️ **Питание:**
• Текущая фаза: {profile.get('current_phase', '❌ Не указана')}
• Ограничения: {profile.get('dietary_restrictions', 'Не указаны')}

📊 **Статистика:**
• Дата регистрации: {profile.get('created_at').strftime('%d.%m.%Y')}
• Онбординг: {'✅ Завершен' if profile.get('onboarding_completed') else '⏳ В процессе'}
"""

    await message.answer(profile_text, reply_markup=profile_kb)

async def start_profile_editing(message: Message, state: FSMContext):
    """Начать редактирование профиля"""
    await message.answer(
        "✏️ **Редактирование профиля**\n\n"
        "Что хотите изменить?\n\n"
        "1️⃣ Рост и вес\n"
        "2️⃣ Текущая фаза питания\n"
        "3️⃣ Ограничения в питании\n\n"
        "Введите номер (1-3) или 'отмена':"
    )
    await state.set_state(ProfileStates.choosing_field)

async def process_field_choice(message: Message, state: FSMContext, pool):
    """Обработка выбора поля для редактирования"""
    choice = message.text.strip()

    if choice.lower() in ['отмена', 'назад']:
        await message.answer("❌ Редактирование отменено", reply_markup=profile_kb)
        await state.clear()
        return

    if choice == "1":
        await message.answer(
            "📏 **Изменение роста и веса**\n\n"
            "Введите в формате: рост,вес\n"
            "Например: 170,85.5"
        )
        await state.set_state(ProfileStates.editing_height_weight)

    elif choice == "2":
        await message.answer(
            "🍽️ **Изменение фазы питания**\n\n"
            "На какой фазе вы сейчас?\n\n"
            "1️⃣ Жидкая (первые 1-2 недели)\n"
            "2️⃣ Пюре (2-4 недели)\n"
            "3️⃣ Мягкая (4-8 недель)\n"
            "4️⃣ Обычная (после 8 недель)\n\n"
            "Введите номер (1-4):"
        )
        await state.set_state(ProfileStates.editing_phase)

    elif choice == "3":
        await message.answer(
            "🚫 **Ограничения в питании**\n\n"
            "Укажите ваши пищевые ограничения или аллергии.\n"
            "Если нет ограничений, напишите 'нет':"
        )
        await state.set_state(ProfileStates.editing_restrictions)

    else:
        await message.answer("❌ Неверный выбор. Введите номер от 1 до 3 или 'отмена'")

async def update_height_weight(message: Message, state: FSMContext, pool):
    """Обновление роста и веса"""
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

        await update_user_onboarding(
            pool,
            message.from_user.id,
            height_cm=height,
            weight_kg=weight
        )

        await message.answer(
            f"✅ **Данные обновлены!**\n\n"
            f"📏 Рост: {height} см\n"
            f"⚖️ Вес: {weight} кг",
            reply_markup=profile_kb
        )
        await state.clear()

    except ValueError:
        await message.answer(
            "❌ Неверный формат. Используйте: рост,вес\n"
            "Например: 170,85.5"
        )

async def update_phase(message: Message, state: FSMContext, pool):
    """Обновление фазы питания"""
    phases = {
        "1": "жидкая",
        "2": "пюре",
        "3": "мягкая",
        "4": "обычная"
    }

    phase_num = message.text.strip()
    if phase_num in phases:
        phase = phases[phase_num]
        await update_user_onboarding(pool, message.from_user.id, current_phase=phase)

        await message.answer(
            f"✅ **Фаза питания обновлена!**\n\n"
            f"🍽️ Текущая фаза: {phase}",
            reply_markup=profile_kb
        )
        await state.clear()
    else:
        await message.answer("❌ Введите номер от 1 до 4")

async def update_restrictions(message: Message, state: FSMContext, pool):
    """Обновление ограничений"""
    restrictions = message.text if message.text.lower() != 'нет' else None
    await update_user_onboarding(pool, message.from_user.id, dietary_restrictions=restrictions)

    restrictions_text = restrictions if restrictions else "отсутствуют"
    await message.answer(
        f"✅ **Ограничения обновлены!**\n\n"
        f"🚫 Ограничения: {restrictions_text}",
        reply_markup=profile_kb
    )
    await state.clear()

async def back_to_main_menu(message: Message):
    """Возврат в главное меню"""
    await message.answer(
        "🏠 Главное меню",
        reply_markup=main_patient_kb
    )