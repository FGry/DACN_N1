package com.bookhub.order;

import lombok.Data;
import java.util.List;

@Data
public class GuestCheckoutDTO {
    private String phone;
    private String address;
    private String paymentMethod;

    // Đã xóa trường note

    // ⭐ QUAN TRỌNG: Phải có trường này thì mới hết lỗi
    private String itemsJson;
}