# Бариатрический Бот

Телеграм-бот для постбариатрической поддержки пациентов. Все сервисы реализованы на Java (Spring Boot).

## 🚀 Быстрый старт

### 1. Подготовка окружения

```bash
git clone <your-repo-url>
cd baria-bot
cp .env.example .env
```

### 2. Настройка

Отредактируйте `.env`:
```bash
POSTGRES_DB=baria
POSTGRES_USER=app
POSTGRES_PASSWORD=app
BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN
```

### 3. Запуск

```bash
docker-compose up -d
```

### 4. Проверка

```bash
docker-compose ps
curl http://localhost:8000/ask -X POST -H 'Content-Type: application/json' -d '{"q":"привет"}'
```

## 📊 Архитектура

### Компоненты

- **Telegram Bot** (Java, Spring Boot)
- **RAG Service** (Java, Spring Boot)
- **PostgreSQL + pgvector**

### Порты

- 5432 — PostgreSQL
- 8000 — RAG сервис
- 8080 — Telegram бот

## 🧪 Тестирование

```bash
mvn test
```

## 🔐 Безопасность

Все медицинские ответы содержат обязательный дисклеймер. При ухудшении самочувствия всегда обращайтесь к врачу.
