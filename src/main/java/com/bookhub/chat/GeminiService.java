package com.bookhub.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String ask(String question) {

        // URL đầy đủ với key


        String url = apiUrl + "?key=" + apiKey;

        // Body theo format API 2.5 Flash
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", question)
                                )
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        // Xử lý response
        List candidates = (List) response.getBody().get("candidates");
        Map first = (Map) candidates.get(0);
        Map content = (Map) first.get("content");
        List parts = (List) content.get("parts");

        return (String) ((Map) parts.get(0)).get("text");
    }
}
