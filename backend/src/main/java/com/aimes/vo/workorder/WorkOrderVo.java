package com.aimes.vo.workorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderVo {

    private Long id;
    private String orderNo;
    private Long planId;
    private String planNo;
    private Long productId;
    private String productName;
    private Integer orderQty;
    private Long routingId;
    private String routeVersion;
    private Long teamId;
    private String teamName;
    private String processName;
    private Integer progress;
    private String status;
    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    private BigDecimal estimatedHours;
    private Integer schedulingRank;
    private String schedulingReason;
    private Long claimUserId;
    private String remark;
    private Long exceptionCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
}
