package com.bookhub.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO độc lập dùng cho câu lệnh JPQL SELECT NEW và tính toán trong Controller.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaleStats {

    private String productName;
    private Long totalQuantity;
    private Long totalRevenue;

    private Double saleRatio;

    public ProductSaleStats(String productName, Long totalQuantity, Long totalRevenue) {
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }
}