package com.aimes.dto.request.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceStatusRequest {
    @NotBlank(message = "设备状态不能为空")
    private String status;
    private String remark;
}
