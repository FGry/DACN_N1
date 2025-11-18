package com.bookhub.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> payload) {

        String message = payload.get("message");

        try {
            String reply = geminiService.ask(message);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi gọi Gemini: " + e.getMessage()));
        }
    }
}
