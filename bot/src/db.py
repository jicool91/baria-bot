import asyncpg
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
            RETURNING id, role
            """,
            tg_id, username, full_name
        )
