package com.bookhub.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderDTO {
    private Integer idOrder;
    private String orderCode;
    private Integer userId;
    private String customerUsername;
    private String customerPhone;
    private Long totalAmount;
    private String totalAmountFormatted;
    private Integer totalProducts;
    private String status;
    private LocalDate date;
    private String dateFormatted;
    private String address;
    private String paymentMethod;
    private String note;
    private List<OrderDetailDTO> productDetails;
    private String guestAccessToken;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class OrderItemDTO {
        private Integer productId;
        private Integer quantity;
    }

    @Data
    public static class OrderDetailDTO {
        private String productName;
        private String productAuthor;
        private Integer quantity;
        private Long priceAtDate;
        private String priceAtDateFormatted;

        // ⭐ TRƯỜNG MỚI: Lưu thành tiền (Giá * Số lượng) đã format
        private String totalPriceFormatted;
    }
}