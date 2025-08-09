import os
import logging
from fastapi import FastAPI
from pydantic import BaseModel
from .raglib import search, ensure_schema, add_sample_data, get_chunks_count
from openai import OpenAI

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434/v1")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.1:8b")

# Создаем клиент OpenAI для работы с Ollama
client = OpenAI(base_url=OLLAMA_BASE_URL, api_key="ollama")

app = FastAPI(title="Baria Bot RAG Service", version="1.0.0")

class AskReq(BaseModel):
    user_id: int
    question: str

class AskResp(BaseModel):
    answer: str
    sources: list[str] = []

SYSTEM_PROMPT = (
    "Ты — ассистент по сопровождению после бариатрической операции. "
    "Отвечай строго на основе предоставленных методичек. "
    "НЕ ставь диагнозы, НЕ давай медикаментозных назначений. "
    "Всегда добавляй дисклеймер о необходимости консультации с врачом при ухудшении состояния."
)

@app.on_event("startup")
async def startup_event():
    """Инициализация при запуске сервиса"""
    try:
        logging.info("🚀 Инициализация RAG сервиса...")
        ensure_schema()

        # Проверяем есть ли данные, если нет - добавляем тестовые
        chunks_count = get_chunks_count()
        if chunks_count == 0:
            logging.info("📚 Добавляем тестовые данные...")
            added = add_sample_data()
            logging.info(f"✅ Добавлено {added} тестовых записей")
        else:
            logging.info(f"✅ В базе уже есть {chunks_count} записей")

        logging.info("🎉 RAG сервис готов к работе")
    except Exception as e:
        logging.error(f"❌ Ошибка инициализации: {e}")
        import traceback
        traceback.print_exc()

@app.get("/")
async def root():
    return {"status": "RAG service is running", "version": "1.0.0"}

@app.get("/health")
async def health_check():
    try:
        logging.info("🔍 Проверка здоровья сервиса...")

        # Проверяем подключение к БД
        chunks_count = get_chunks_count()
        logging.info(f"📊 Найдено {chunks_count} чанков в базе")

        if chunks_count == 0:
            logging.info("📚 Пытаемся добавить тестовые данные...")
            added = add_sample_data()
            chunks_count = get_chunks_count()
            logging.info(f"✅ Добавлено {added} записей, всего: {chunks_count}")

        return {"status": "healthy", "db_connection": "ok", "chunks_count": chunks_count}
    except Exception as e:
        logging.error(f"❌ Ошибка health check: {e}")
        import traceback
        traceback.print_exc()
        return {"status": "unhealthy", "error": str(e)}

@app.post("/ask", response_model=AskResp)
async def ask(req: AskReq):
    try:
        # Ищем релевантные фрагменты
        contexts = search(req.question, top_k=5)

        if not contexts:
            return AskResp(
                answer="Извините, не найдено релевантной информации в базе знаний. Обратитесь к вашему врачу за консультацией.",
                sources=[]
            )

        context_text = "\n\n".join([f"Источник: {c['source']}\n{c['content']}" for c in contexts])
        sources = list(set([c['source'] for c in contexts]))

        user_prompt = (
            f"Контекст из медицинских протоколов:\n{context_text}\n\n"
            f"Вопрос пациента: {req.question}\n\n"
            f"Дай краткий ответ на основе только предоставленного контекста. "
            f"Если информации недостаточно, направь к врачу."
        )

        # Запрос к LLM
        resp = client.chat.completions.create(
            model=OLLAMA_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.2,
            max_tokens=500
        )

        answer = resp.choices[0].message.content.strip()

        # Добавляем обязательный дисклеймер
        disclaimer = "\n\n⚠️ Это информационная справка на основе медицинских протоколов. При ухудшении самочувствия обратитесь к врачу."
        final_answer = answer + disclaimer

        return AskResp(answer=final_answer, sources=sources)

    except Exception as e:
        logging.error(f"Ошибка в /ask: {e}")
        return AskResp(
            answer="Извините, произошла ошибка при обработке запроса. Попробуйте позже или обратитесь к врачу.",
            sources=[]
        )