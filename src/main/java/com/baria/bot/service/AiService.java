package com.baria.bot.service;

import com.baria.bot.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {
    @Value("${rag.url:http://localhost:8000/ask}")
    private String ragUrl;

    private final RestTemplate restTemplate;

    public AiService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public String ask(String question) {
        return askQuestion(question, null);
    }

    public String askQuestion(String prompt, User user) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("q", prompt);
            if (user != null) {
                Map<String, Object> u = new HashMap<>();
                u.put("phase", user.getPhase() != null ? user.getPhase().getCode() : null);
                u.put("restrictions", user.getRestrictions());
                u.put("goal", user.getGoal() != null ? user.getGoal().getCode() : null);
                body.put("user", u);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(ragUrl, entity, Map.class);
            Map bodyResp = resp.getBody();
            Object answer = bodyResp != null ? bodyResp.get("answer") : null;
            Object sourcesObj = bodyResp != null ? bodyResp.get("sources") : null;
            String text = answer != null ? answer.toString() : "Нет ответа";
            String sources = "";
            if (sourcesObj instanceof Iterable<?> iter) {
                StringBuilder sb = new StringBuilder();
                for (Object s : iter) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(s.toString());
                }
                sources = sb.toString();
            }
            if (sources.isEmpty()) {
                sources = "неизвестно";
            }
            return text + "\n\nОсновано на: " + sources + ". Это информационная справка, не медрекомендация.";
        } catch (Exception e) {
            return "Сервис недоступен";
        }
    }
}

