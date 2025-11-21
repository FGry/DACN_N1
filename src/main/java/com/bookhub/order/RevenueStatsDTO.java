package com.bookhub.order;

import lombok.AllArgsConstructor;
import lombok.Builder; // Đã thêm import này
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // <--- ĐÃ THÊM: Annotation này giúp sửa lỗi .builder()
public class RevenueStatsDTO {

    private Long totalRevenue;
    private Long totalDeliveredOrders;
    private List<ProductSaleStats> topSellingProducts;

    private List<DataPoint> monthlyRevenue;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder // <--- ĐÃ THÊM: Để tiện tạo đối tượng DataPoint nếu cần
    public static class DataPoint {
        private String label;
        private Double value;
    }
}