package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceInspectionItemRequest {
    @NotBlank(message = "点检项名称不能为空")
    private String itemName;
    @NotNull(message = "是否正常不能为空")
    private Boolean isNormal;
    private String remark;
}
