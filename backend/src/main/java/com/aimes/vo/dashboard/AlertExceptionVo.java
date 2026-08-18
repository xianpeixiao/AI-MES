package com.aimes.vo.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertExceptionVo {

    private Long id;
    private String eventNo;
    private String eventType;
    private Long workOrderId;
    private String status;
    private String description;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurTime;
}
