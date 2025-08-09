# bot/src/handlers/__init__.py
"""
Модуль обработчиков для телеграм-бота
"""

from . import onboarding
from . import profile  
from . import ai_assistant
from . import journal

__all__ = ['onboarding', 'profile', 'ai_assistant', 'journal']
