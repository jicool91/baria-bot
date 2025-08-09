from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    BOT_TOKEN: str
    DB_URL: str
    RAG_URL: str = "http://rag:8000/ask"  # Исправлен URL (был 8081)

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
    }

settings = Settings()