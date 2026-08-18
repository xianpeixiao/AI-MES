package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRepairHandleRequest {
    @NotBlank(message = "维修措施不能为空")
    private String repairAction;
    @NotBlank(message = "维修结果不能为空")
    private String repairResult;
    private String remark;
}
