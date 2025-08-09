import re
from typing import Tuple, List, Dict

class RedFlagDetector:
    """Детектор критических симптомов после бариатрической операции"""

    RED_FLAGS = {
        "severe_pain": {
            "keywords": ["боль", "болит", "больно", "невыносимо", "очень больно"],
            "patterns": [r"боль.*([7-9]|10).*из.*10", r"([7-9]|10).*из.*10.*боль"],
            "severity": 10,
            "message": "🚨 КРИТИЧНО: Сильная боль (7+ из 10)"
        },
        "blood_vomit": {
            "keywords": ["рвота с кровью", "кровь в рвоте", "рвет кровью", "кровавая рвота"],
            "patterns": [r"рво.*кров", r"кров.*рво"],
            "severity": 10,
            "message": "🚨 КРИТИЧНО: Рвота с кровью"
        },
        "high_fever": {
            "keywords": ["температура", "жар", "лихорадка", "горячий"],
            "patterns": [r"температур.*3[89]", r"3[89].*градус", r"температур.*40"],
            "severity": 9,
            "message": "🚨 КРИТИЧНО: Высокая температура (38.5°C+)"
        },
        "black_stool": {
            "keywords": ["черный стул", "темный стул", "черная какашка", "мелена"],
            "patterns": [r"черн.*стул", r"темн.*стул", r"стул.*черн"],
            "severity": 9,
            "message": "🚨 КРИТИЧНО: Черный стул (возможно кровотечение)"
        },
        "chest_pain": {
            "keywords": ["боль в груди", "грудная боль", "сердце болит", "боль в сердце"],
            "patterns": [r"груд.*бол", r"бол.*груд", r"сердц.*бол"],
            "severity": 9,
            "message": "🚨 КРИТИЧНО: Боль в груди"
        },
        "breathing_problems": {
            "keywords": ["не могу дышать", "трудно дышать", "одышка", "задыхаюсь"],
            "patterns": [r"не.*дыш", r"трудн.*дыш", r"задых"],
            "severity": 9,
            "message": "🚨 КРИТИЧНО: Проблемы с дыханием"
        },
        "constant_vomiting": {
            "keywords": ["постоянная рвота", "все время рвет", "не могу есть", "ничего не держится"],
            "patterns": [r"постоян.*рво", r"все.*врем.*рво", r"ничего.*не.*держ"],
            "severity": 8,
            "message": "⚠️ ВНИМАНИЕ: Постоянная рвота"
        },
        "dehydration": {
            "keywords": ["обезвоживание", "сухость во рту", "не пью", "головокружение"],
            "patterns": [r"сух.*рот", r"не.*п.*вод", r"головокруж"],
            "severity": 7,
            "message": "⚠️ ВНИМАНИЕ: Признаки обезвоживания"
        }
    }

    @classmethod
    def check_symptoms(cls, text: str) -> Tuple[bool, List[Dict], int]:
        """
        Проверяет текст на наличие red-flags

        Returns:
            (is_critical, detected_flags, max_severity)
        """
        text_lower = text.lower()
        detected_flags = []
        max_severity = 0

        for flag_type, flag_data in cls.RED_FLAGS.items():
            flag_detected = False

            # Проверяем ключевые слова
            for keyword in flag_data["keywords"]:
                if keyword.lower() in text_lower:
                    flag_detected = True
                    break

            # Проверяем паттерны
            if not flag_detected:
                for pattern in flag_data["patterns"]:
                    if re.search(pattern, text_lower):
                        flag_detected = True
                        break

            if flag_detected:
                detected_flags.append({
                    "type": flag_type,
                    "severity": flag_data["severity"],
                    "message": flag_data["message"]
                })
                max_severity = max(max_severity, flag_data["severity"])

        is_critical = max_severity >= 8
        return is_critical, detected_flags, max_severity

    @classmethod
    def format_warning(cls, detected_flags: List[Dict]) -> str:
        """Форматирует предупреждение о критических симптомах"""
        if not detected_flags:
            return ""

        warning = "🚨 ОБНАРУЖЕНЫ КРИТИЧЕСКИЕ СИМПТОМЫ:\n\n"
        for flag in detected_flags:
            warning += f"• {flag['message']}\n"

        warning += "\n🏥 НЕМЕДЛЕННО ОБРАТИТЕСЬ К ВРАЧУ ИЛИ ВЫЗОВИТЕ СКОРУЮ ПОМОЩЬ!\n"
        warning += "📞 Скорая помощь: 103 или 112"

        return warning

# Тестирование
if __name__ == "__main__":
    test_cases = [
        "У меня боль 8 из 10, очень больно",
        "Рвота с кровью уже второй день",
        "Температура 39 градусов",
        "Черный стул сегодня утром",
        "Все в порядке, чувствую себя хорошо"
    ]

    for case in test_cases:
        is_critical, flags, severity = RedFlagDetector.check_symptoms(case)
        print(f"Текст: {case}")
        print(f"Критично: {is_critical}, Тяжесть: {severity}")
        if flags:
            print(RedFlagDetector.format_warning(flags))
        print("-" * 50)