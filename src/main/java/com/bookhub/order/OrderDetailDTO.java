package com.bookhub.order;

import lombok.Data;

@Data
public class OrderDetailDTO {
    private String productName;
    private String productAuthor;
    private String productImageUrl;
    private Integer quantity;
    private Long priceAtDate; // price_date
    private String priceAtDateFormatted;
}