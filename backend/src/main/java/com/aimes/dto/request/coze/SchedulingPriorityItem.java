package com.aimes.dto.request.coze;

import lombok.Data;

@Data
public class SchedulingPriorityItem {
    private Integer rank;
    private String workOrderCode;
    private String workOrderNo;
    private Integer priority;
    private String priorityLabel;
    private String reason;
}
