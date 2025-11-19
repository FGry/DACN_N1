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

    public String ask(String question, String knowledgeText) {

        // Prompt rõ ràng hơn: bot chỉ được trả 2–3 dòng, xuống dòng đẹp, mỗi dòng 10–20 từ
        String prompt = """
        Bạn là trợ lý bán hàng của BookHub.
        Dữ liệu dự án: %s
        Quy tắc trả lời:
        - Trả lời 2–3 dòng.
        - Mỗi dòng 10–20 từ.
        - Xuống dòng đẹp, dễ đọc.
        - Nếu khách hỏi nhiều sản phẩm, liệt kê theo số thứ tự.
        Câu hỏi khách: %s
        """.formatted(knowledgeText, question);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(apiUrl + "?key=" + apiKey,
                HttpMethod.POST, entity, Map.class);

        List candidates = (List) response.getBody().get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "Xin lỗi, không nhận được phản hồi từ Gemini.";
        }

        Map first = (Map) candidates.get(0);
        Map content = (Map) first.get("content");
        List parts = (List) content.get("parts");

        String text = (String) ((Map) parts.get(0)).get("text");


        text = text.replaceAll("(\\d+\\.)\\s*", "\n$1 ") // Bắt số thứ tự (vd: 1.) và thêm xuống dòng trước nó
                .replaceAll("(?i)\\s*•\\s*", "\n• ")       // Bắt bullet point (•) và thêm xuống dòng trước nó
                .replaceAll("\\s*•\\s*", "\n• ")
                .replaceAll("\\n+", "\n")                  // Loại bỏ nhiều xuống dòng liên tiếp
                .trim();

        return text;
    }
}