#!/bin/bash

echo "🚀 Запуск Baria Bot..."

# Проверяем .env файл
if [ ! -f .env ]; then
    echo "⚠️ Файл .env не найден. Создаю из .env.example..."
    cp .env.example .env
    echo "❌ ВНИМАНИЕ: Замените REPLACE_WITH_YOUR_TOKEN на реальный токен бота в .env файле"
    exit 1
fi

# Проверяем что токен заменен
if grep -q "REPLACE_WITH_YOUR_TOKEN" .env; then
    echo "❌ ОШИБКА: Замените REPLACE_WITH_YOUR_TOKEN на реальный токен бота в .env файле"
    exit 1
fi

echo "✅ Конфигурация найдена"

# Останавливаем все контейнеры
echo "🛑 Останавливаем существующие контейнеры..."
docker-compose down

# Запускаем базовые сервисы
echo "🔧 Запускаем базовые сервисы (db, ollama)..."
docker-compose up -d db ollama

# Ждем готовности БД
echo "⏳ Ждем готовности базы данных..."
sleep 10

# Ждем готовности Ollama (проверяем порт)
echo "⏳ Ждем готовности Ollama..."
max_attempts=24
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker-compose exec -T ollama sh -c "test -S /tmp/ollama.sock" 2>/dev/null; then
        echo "✅ Ollama готов"
        break
    fi

    # Альтернативная проверка через логи
    if docker-compose logs ollama 2>/dev/null | grep -q "Listening on"; then
        echo "✅ Ollama готов (проверено через логи)"
        break
    fi

    sleep 5
    attempt=$((attempt + 1))
    echo "   Попытка $attempt/$max_attempts..."
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Ollama не готов после $max_attempts попыток"
    echo "Логи Ollama:"
    docker-compose logs ollama
    exit 1
fi

# Загружаем модель если нужно
echo "📥 Загружаем модель llama3.1:8b..."
docker-compose exec -T ollama ollama pull llama3.1:8b

# Запускаем RAG сервис
echo "🤖 Запускаем RAG сервис..."
docker-compose up -d rag

# Ждем готовности RAG
echo "⏳ Ждем готовности RAG сервиса..."
max_attempts=12
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -f http://localhost:8000/health >/dev/null 2>&1; then
        echo "✅ RAG сервис готов"
        break
    fi
    sleep 5
    attempt=$((attempt + 1))
    echo "   Попытка $attempt/$max_attempts..."
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ RAG сервис не готов после $max_attempts попыток"
    echo "Логи RAG:"
    docker-compose logs rag
    echo ""
    echo "Попробуйте запустить RAG вручную:"
    echo "  docker-compose up rag"
    exit 1
fi

# Запускаем бота
echo "🤖 Запускаем Telegram бота..."
docker-compose up -d bot

# Проверяем статус бота
sleep 5
if docker-compose ps bot | grep -q "Up"; then
    echo "✅ Telegram бот запущен"
else
    echo "❌ Проблема с запуском бота. Логи:"
    docker-compose logs bot
    exit 1
fi

echo ""
echo "🎉 Все сервисы запущены!"
echo ""
echo "📊 Статус сервисов:"
docker-compose ps
echo ""
echo "🔍 Полезные команды:"
echo "  docker-compose logs bot    # Логи бота"
echo "  docker-compose logs rag    # Логи RAG сервиса"
echo "  curl http://localhost:8000/health  # Проверка RAG"
echo "  docker-compose ps          # Статус всех сервисов"
echo ""
echo "🧪 Тестирование RAG API:"
curl -X POST http://localhost:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"user_id": 1, "question": "Что можно есть на жидкой фазе?"}' \
  2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  (требуется curl и python3 для теста)"
echo ""
echo "✅ Готово! Найдите своего бота в Telegram и напишите /start"