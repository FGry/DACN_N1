package com.bookhub.payment;

import com.bookhub.order.Order;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import vn.payos.PayOS;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayOSService {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String createPaymentLink(Order order) {
        try {
            // 1. TẠO ORDER CODE THEO CÔNG THỨC TOÁN HỌC
            // OrderCode = (OrderID * 10000) + (3 số cuối thời gian)
            // Ví dụ: OrderID 15 -> 150000 + 123 = 150123
            // Cách này đảm bảo OrderCode là duy nhất và có thể tính ngược lại ra ID gốc
            long timePart = System.currentTimeMillis() % 10000;
            long orderCode = (order.getId_order() * 10000L) + timePart;

            int amount = order.getTotal().intValue();
            String description = "Thanh toan don " + order.getId_order();

            // 2. Tạo chữ ký (Signature) thủ công
            SortedMap<String, String> signatureData = new TreeMap<>();
            signatureData.put("amount", String.valueOf(amount));
            signatureData.put("cancelUrl", cancelUrl);
            signatureData.put("description", description);
            signatureData.put("orderCode", String.valueOf(orderCode));
            signatureData.put("returnUrl", returnUrl);

            String signatureQuery = buildQueryString(signatureData);
            String signature = generateHmacSHA256(signatureQuery, checksumKey);

            // 3. Tạo JSON Body request
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("orderCode", orderCode);
            requestBody.put("amount", amount);
            requestBody.put("description", description);
            requestBody.put("cancelUrl", cancelUrl);
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("signature", signature);
            // KHÔNG gửi items để tránh lỗi logic voucher

            // 4. Gửi Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            String apiResponse = restTemplate.postForObject(
                    "https://api-merchant.payos.vn/v2/payment-requests",
                    request,
                    String.class
            );

            // 5. Parse kết quả
            JsonNode rootNode = objectMapper.readTree(apiResponse);
            String code = rootNode.path("code").asText();

            if (!"00".equals(code)) {
                throw new RuntimeException("PayOS Error [" + code + "]: " + rootNode.path("desc").asText());
            }

            return rootNode.path("data").path("checkoutUrl").asText();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("❌ LỖI GỌI API PAYOS: " + responseBody);
            throw new RuntimeException("Lỗi PayOS: " + responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi tạo link: " + e.getMessage());
        }
    }

    private String buildQueryString(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private String generateHmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}