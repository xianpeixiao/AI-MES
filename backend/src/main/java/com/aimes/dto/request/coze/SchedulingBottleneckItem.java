package com.aimes.dto.request.coze;

import lombok.Data;

@Data
public class SchedulingBottleneckItem {
    private String processName;
    private Integer loadRate;
    private String suggestion;
}
