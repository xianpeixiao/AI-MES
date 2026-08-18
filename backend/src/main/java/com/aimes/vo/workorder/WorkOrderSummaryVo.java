package com.aimes.vo.workorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderSummaryVo {

    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private Integer orderQty;
    private String processName;
    private Integer progress;
    private String status;
    private String teamName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    private BigDecimal estimatedHours;
    private Integer schedulingRank;
}
