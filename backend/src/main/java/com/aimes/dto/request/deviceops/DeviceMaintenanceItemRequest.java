package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceMaintenanceItemRequest {
    @NotBlank(message = "保养项名称不能为空")
    private String itemName;
    @NotNull(message = "是否完成不能为空")
    private Boolean done;
    private String remark;
}
