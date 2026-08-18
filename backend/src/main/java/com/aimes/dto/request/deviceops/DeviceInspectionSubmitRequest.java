package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeviceInspectionSubmitRequest {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    private Long planId;
    @NotEmpty(message = "点检项不能为空")
    private List<DeviceInspectionItemRequest> items;
    private String remark;
}
