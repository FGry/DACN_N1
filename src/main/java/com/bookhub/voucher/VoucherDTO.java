package com.bookhub.voucher;

import lombok.Data;

// DTO này có các trường khớp 1:1 với mảng JS trong voucher.html
@Data
public class VoucherDTO {
    private Integer id;
    private String code;
    private String title;
    private String description;
    private String discountValue;
    private String discountType;
    private String category;
    private String type;
    private String minOrder;
    private String endDate; // Sẽ là chuỗi "YYYY-MM-DD"
    private String status;
    private String requirements;
    private Integer quantity;
}