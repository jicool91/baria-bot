package com.baria.rag.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RagController {
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(Map.of(
                "answer", "stub",
                "sources", List.of(),
                "used_chunks", List.of(),
                "safety", Map.of(),
                "tokens", Map.of("prompt", 0, "completion", 0)
        ));
    }
}
