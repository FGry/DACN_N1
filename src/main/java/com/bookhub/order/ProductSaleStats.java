package com.bookhub.order;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO dùng cho câu lệnh JPQL SELECT NEW.
 * Đã xóa Lombok @NoArgsConstructor và @AllArgsConstructor để đảm bảo
 * JPA gọi đúng constructor.
 */
@Getter
@Setter
public class ProductSaleStats {

    private String productName;
    private Long totalQuantity;
    private Long totalRevenue;

    private Double saleRatio; // Trường này sẽ được tính toán sau trong service/controller

    // 1. Constructor rỗng (BẮT BUỘC phải có)
    public ProductSaleStats() {
    }

    // 2. Constructor mà JPQL @Query sẽ gọi (BẮT BUỘC phải khớp 100%)
    public ProductSaleStats(String productName, Long totalQuantity, Long totalRevenue) {
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }
}