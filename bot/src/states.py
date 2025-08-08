from aiogram.fsm.state import State, StatesGroup

class RegStates(StatesGroup):
    waiting_full_name = State()
    waiting_surgery_date = State()
