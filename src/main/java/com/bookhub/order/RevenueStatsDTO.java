package com.bookhub.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsDTO {

    private Long totalRevenue;
    private Long totalDeliveredOrders;
    private List<ProductSaleStats> topSellingProducts;

    private List<DataPoint> monthlyRevenue;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String label;
        private Double value;
    }
}