package com.bookhub.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticDTO {
    private String period;
    private Long totalRevenue;
    private Long totalInvoices;
    private Double averageOrderValue;

    // Thuộc tính đã định dạng
    private String totalRevenueFormatted;
    private String averageOrderValueFormatted;
}