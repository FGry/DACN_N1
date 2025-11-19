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

    @Autowired
    private ProjectKnowledgeService knowledgeService;

    // Helper method to check if the message requests a product list
    private boolean isProductListRequest(String message) {
        if (message == null) return false;
        String lowerCaseMessage = message.toLowerCase();
        return lowerCaseMessage.contains("5 sản phẩm") ||
                lowerCaseMessage.contains("gợi ý 5") ||
                lowerCaseMessage.contains("danh sách 5") ||
                lowerCaseMessage.contains("sản phẩm ngẫu nhiên 5");
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> payload) {

        String message = payload.get("message");
        String reply;

        try {
            // 1. Xử lý yêu cầu lấy 5 sản phẩm trực tiếp
            if (isProductListRequest(message)) {
                int productCount = 5;
                String formattedProducts = knowledgeService.formatProducts(
                        knowledgeService.getRandomProducts(productCount)
                );

                // SỬA LỖI: Thêm "\n\n" sau dấu hai chấm để tách dòng đầu tiên
                reply = "Chào bạn, BookHub xin gợi ý " + productCount + " sản phẩm sau:\n\n" + formattedProducts;

                return ResponseEntity.ok(Map.of("reply", reply));
            }

            // 2. Gọi Gemini như bình thường
            String knowledgeText = knowledgeService.buildKnowledge();
            reply = geminiService.ask(message, knowledgeText);

            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Lỗi khi xử lý yêu cầu: " + e.getMessage()));
        }
    }
}