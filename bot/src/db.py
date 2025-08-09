import asyncpg
from datetime import datetime
from .settings import settings

async def get_pool():
    return await asyncpg.create_pool(dsn=settings.DB_URL, min_size=1, max_size=5)

async def upsert_user(pool, tg_id: int, username: str | None, full_name: str | None):
    async with pool.acquire() as con:
        return await con.fetchrow(
            """
            INSERT INTO users(tg_id, username, full_name)
            VALUES ($1, $2, $3)
            ON CONFLICT (tg_id) DO UPDATE SET
              username = EXCLUDED.username,
              full_name = EXCLUDED.full_name
            RETURNING id, role, onboarding_completed
            """,
            tg_id, username, full_name
        )

async def update_user_onboarding(pool, tg_id: int, **kwargs):
    """Обновляет данные пользователя в процессе онбординга"""
    async with pool.acquire() as con:
        # Строим динамический запрос
        fields = []
        values = []
        for i, (key, value) in enumerate(kwargs.items(), 2):
            fields.append(f"{key} = ${i}")
            values.append(value)

        if not fields:
            return

        query = f"""
            UPDATE users
            SET {', '.join(fields)}
            WHERE tg_id = $1
        """

        await con.execute(query, tg_id, *values)

async def complete_onboarding(pool, tg_id: int):
    """Завершает онбординг пользователя"""
    async with pool.acquire() as con:
        await con.execute(
            "UPDATE users SET onboarding_completed = TRUE WHERE tg_id = $1",
            tg_id
        )

async def verify_doctor_code(pool, code: str):
    """Проверяет валидность кода врача"""
    async with pool.acquire() as con:
        return await con.fetchrow(
            """
            SELECT dc.code, d.id as doctor_id, u.full_name as doctor_name
            FROM doctor_codes dc
            JOIN doctors d ON dc.doctor_id = d.id
            JOIN users u ON d.user_id = u.id
            WHERE dc.code = $1 AND dc.active = TRUE
            """,
            code
        )

async def get_user_profile(pool, tg_id: int):
    """Получает полный профиль пользователя"""
    async with pool.acquire() as con:
        return await con.fetchrow(
            """
            SELECT * FROM users WHERE tg_id = $1
            """,
            tg_id
        )

async def save_red_flag(pool, user_id: int, symptoms: str, severity: int):
    """Сохраняет критический симптом"""
    async with pool.acquire() as con:
        return await con.fetchrow(
            """
            INSERT INTO red_flags(user_id, symptoms, severity, auto_escalated)
            VALUES ($1, $2, $3, TRUE)
            RETURNING id, created_at
            """,
            user_id, symptoms, severity
        )

async def save_journal_entry(pool, tg_id: int, weight_kg: float, mood: int, symptoms: str):
    """Сохраняет запись в журнале"""
    async with pool.acquire() as con:
        # Сначала получаем user_id
        user = await con.fetchrow("SELECT id FROM users WHERE tg_id = $1", tg_id)
        if not user:
            return None

        return await con.fetchrow(
            """
            INSERT INTO journals(patient_id, weight_kg, mood, symptoms)
            VALUES ($1, $2, $3, $4)
            RETURNING id, entry_date, created_at
            """,
            user['id'], weight_kg, mood, symptoms
        )