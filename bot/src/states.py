from aiogram.fsm.state import State, StatesGroup

class RegStates(StatesGroup):
    # Основная регистрация
    waiting_full_name = State()
    waiting_surgery_date = State()
    waiting_doctor_code = State()
    waiting_height_weight = State()
    waiting_phase = State()
    waiting_restrictions = State()
    waiting_consent = State()

    # Дополнительные состояния
    completed = State()

class JournalStates(StatesGroup):
    waiting_journal_data = State()

class AskAIStates(StatesGroup):
    waiting_question = State()

class RedFlagStates(StatesGroup):
    emergency_detected = State()
    waiting_doctor_contact = State()