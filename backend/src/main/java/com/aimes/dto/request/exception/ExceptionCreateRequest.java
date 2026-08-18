package com.aimes.dto.request.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionCreateRequest {
    @NotBlank(message = "异常类型不能为空")
    private String eventType;
    @NotNull(message = "关联工单不能为空")
    private Long workOrderId;
    private Long deviceId;
    @NotBlank(message = "异常描述不能为空")
    private String description;
    @NotNull(message = "发生时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurTime;
}
