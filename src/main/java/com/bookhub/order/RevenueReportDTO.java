package com.bookhub.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    private String period;
    private Long revenue;
    private Long profit;
    private Double growthRate;
}