# bot/src/handlers/profile.py
from datetime import datetime
from aiogram.types import Message
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from ..db import get_user_profile, update_user_onboarding
from ..keyboards import main_patient_kb

# Глобальная переменная для pool (будет установлена в main.py)
pool = None

class ProfileStates(StatesGroup):
    choosing_field = State()
    editing_height_weight = State()
    editing_phase = State()
    editing_restrictions = State()

async def show_profile(message: Message, db_pool):
    """Показать профиль пользователя"""
    global pool
    if pool is None:
        pool = db_pool

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

    # Прогресс-бар
    progress_bar = "🟩" * (completeness // 20) + "⬜" * (5 - completeness // 20)

    profile_text = f"""
👤 **Ваш профиль**

📊 **Заполненность:** {completeness}% {progress_bar}

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

✏️ **Для редактирования напишите:** `редактировать профиль`
"""

    await message.answer(profile_text)

async def handle_profile_edit(message: Message):
    """Инструкции по редактированию профиля"""
    await message.answer(
        "✏️ **Редактирование профиля**\n\n"
        "Доступные команды для быстрого обновления:\n\n"
        "📅 **Дата операции:**\n"
        "`дата операции: ДД.ММ.ГГГГ`\n"
        "Пример: `дата операции: 15.06.2024`\n\n"
        "📏 **Рост и вес:**\n"
        "`рост вес: рост,вес`\n"
        "Пример: `рост вес: 170,75.5`\n\n"
        "🍽️ **Фаза питания:**\n"
        "`фаза: номер`\n"
        "Пример: `фаза: 3`\n"
        "(1-жидкая, 2-пюре, 3-мягкая, 4-обычная)\n\n"
        "🚫 **Ограничения:**\n"
        "`ограничения: текст`\n"
        "Пример: `ограничения: непереносимость лактозы`\n\n"
        "Просто напишите команду в чат!"
    )

async def handle_simple_profile_updates(message: Message, db_pool):
    """Обработка команд редактирования профиля"""
    global pool
    if pool is None:
        pool = db_pool

    text = message.text.lower().strip()

    try:
        # Обновление даты операции
        if text.startswith('дата операции:'):
            date_str = text.split(':', 1)[1].strip()
            surgery_date = datetime.strptime(date_str, "%d.%m.%Y").date()
            
            # Проверяем что дата не в будущем
            if surgery_date > datetime.now().date():
                await message.answer("❌ Дата операции не может быть в будущем")
                return True
                
            await update_user_onboarding(pool, message.from_user.id, surgery_date=surgery_date)
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
                    await update_user_onboarding(
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
                await update_user_onboarding(pool, message.from_user.id, current_phase=phase)
                await message.answer(f"✅ Фаза питания обновлена: {phase}")
                return True
            else:
                raise ValueError("Неверная фаза")

        # Обновление ограничений
        elif text.startswith('ограничения:'):
            restrictions = text.split(':', 1)[1].strip()
            restrictions = restrictions if restrictions.lower() != 'нет' else None
            await update_user_onboarding(pool, message.from_user.id, dietary_restrictions=restrictions)
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

async def start_profile_editing(message: Message, state: FSMContext):
    """Начать редактирование профиля через FSM"""
    await message.answer(
        "✏️ **Редактирование профиля**\n\n"
        "Что хотите изменить?\n\n"
        "1️⃣ Рост и вес\n"
        "2️⃣ Текущая фаза питания\n"
        "3️⃣ Ограничения в питании\n\n"
        "Введите номер (1-3) или 'отмена':"
    )
    await state.set_state(ProfileStates.choosing_field)

async def process_field_choice(message: Message, state: FSMContext, db_pool):
    """Обработка выбора поля для редактирования"""
    global pool
    if pool is None:
        pool = db_pool

    choice = message.text.strip()

    if choice.lower() in ['отмена', 'назад']:
        await message.answer("❌ Редактирование отменено", reply_markup=main_patient_kb)
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

async def update_height_weight(message: Message, state: FSMContext, db_pool):
    """Обновление роста и веса"""
    global pool
    if pool is None:
        pool = db_pool

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
            reply_markup=main_patient_kb
        )
        await state.clear()

    except ValueError:
        await message.answer(
            "❌ Неверный формат. Используйте: рост,вес\n"
            "Например: 170,85.5"
        )

async def update_phase(message: Message, state: FSMContext, db_pool):
    """Обновление фазы питания"""
    global pool
    if pool is None:
        pool = db_pool

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
            reply_markup=main_patient_kb
        )
        await state.clear()
    else:
        await message.answer("❌ Введите номер от 1 до 4")

async def update_restrictions(message: Message, state: FSMContext, db_pool):
    """Обновление ограничений"""
    global pool
    if pool is None:
        pool = db_pool

    restrictions = message.text if message.text.lower() != 'нет' else None
    await update_user_onboarding(pool, message.from_user.id, dietary_restrictions=restrictions)

    restrictions_text = restrictions if restrictions else "отсутствуют"
    await message.answer(
        f"✅ **Ограничения обновлены!**\n\n"
        f"🚫 Ограничения: {restrictions_text}",
        reply_markup=main_patient_kb
    )
    await state.clear()
