package com.aimes.vo.exception;

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
public class ExceptionVo {

    private Long id;
    private String eventNo;
    private String eventType;
    private Long workOrderId;
    private String workOrderNo;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String description;
    private String status;
    private Long reporterId;
    private String reporterName;
    private Long handlerId;
    private String handlerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    private String handleAction;
    private String handleResult;
}
