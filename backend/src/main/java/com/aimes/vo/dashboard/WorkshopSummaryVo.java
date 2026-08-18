package com.aimes.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopSummaryVo {

    private Integer completionRate;
    private Long completedOrders;
    private Long totalOrders;
    private String lineName;
    private String shiftName;
    private Long todayOutput;
    private Integer onTimeRate;
    private Long activeLines;
}
