package com.bookhub.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSalesDTO {
    private Integer idProduct;
    private String productName;
    private Long quantitySold;
    private Long totalRevenue;
    private String totalRevenueFormatted;
    private Double percentageOfTotal; // % tổng doanh thu của sản phẩm
}