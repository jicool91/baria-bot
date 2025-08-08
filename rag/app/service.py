import os
from fastapi import FastAPI
from pydantic import BaseModel
from .raglib import search
from openai import OpenAI

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434/v1")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.1:8b")

client = OpenAI(base_url=OLLAMA_BASE_URL, api_key="ollama")

app = FastAPI()

class AskReq(BaseModel):
    user_id: int
    question: str

class AskResp(BaseModel):
    answer: str

SYSTEM_PROMPT = (
    "Ты — ассистент по сопровождению после бариатрической операции. Отвечай строго по методичкам. "
    "Не ставь диагнозы, не давай медикаментозных назначений. Если нет данных — скажи, что нужно уточнить у врача."
)

@app.post("/ask", response_model=AskResp)
async def ask(req: AskReq):
    contexts = search(req.question, top_k=5)
    context_text = "\n\n".join([c["content"] for c in contexts])

    user_prompt = (
        f"{SYSTEM_PROMPT}\n\n"
        f"Вопрос: {req.question}\n\n"
        f"Извлечённые выдержки из методичек:\n{context_text}\n\n"
        f"Ответь кратко и по делу. Если есть риски — предложи связаться с врачом."
    )

    resp = client.chat.completions.create(
        model=OLLAMA_MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.2,
    )
    answer = resp.choices[0].message.content.strip()
    return AskResp(answer=answer if answer else "Нет подходящих данных, обратитесь к врачу.")
