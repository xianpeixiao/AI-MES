package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceRepairCreateRequest {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    @NotBlank(message = "故障原因不能为空")
    private String faultReason;
    private String faultCode;
    private String description;
    private Long eventId;
    private String remark;
}
