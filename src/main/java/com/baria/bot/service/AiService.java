package com.baria.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AiService {
    @Value("${rag.url:http://localhost:8000/ask}")
    private String ragUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String ask(String question) {
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(ragUrl, Map.of("question", question), Map.class);
            Object answer = resp.getBody() != null ? resp.getBody().get("answer") : null;
            return answer != null ? answer.toString() : "Нет ответа";
        } catch (Exception e) {
            return "Сервис недоступен";
        }
    }
}

