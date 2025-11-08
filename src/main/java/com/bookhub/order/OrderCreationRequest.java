package com.bookhub.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreationRequest {
    private Integer userId;
    private String address;
    private String phone;
    private String paymentMethod;
    private String note;
    private Integer voucherId;
    private Long totalAmount;

    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Integer productId;
        private Long quantity;
        private Long priceAtDate;
    }
}