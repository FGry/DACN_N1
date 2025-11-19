package com.bookhub.order;

import lombok.Data;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// Sử dụng @Data của Lombok để tạo getters, setters, toString
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Giả định có @Valid trên Controller cho class này
public class OrderCreationRequest {
    private String address;
    private String phone;
    private String paymentMethod;
    private String note;

    // Tổng tiền thanh toán cuối cùng (sau khi giảm giá)
    private Long totalAmount;

    // Thay thế voucherId bằng voucherCode (String)
    private String voucherCode;

    // Số tiền giảm giá thực tế (dùng để re-validate ở backend)
    private Long voucherDiscount;

    private List<OrderItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Integer productId;
        // Giá sản phẩm tại thời điểm đặt hàng
        private Long priceAtDate;
        // Số lượng sản phẩm
        private Long quantity;
    }
}