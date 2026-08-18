package com.aimes.dto.request.coze;

import lombok.Data;

@Data
public class SchedulingDispatchItem {
    private String workOrderCode;
    private String workOrderNo;
    private String teamName;
    private String startTime;
    private String hours;
}
