package com.aimes.dto.request.deviceops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceInspectionPlanSaveRequest {
    private String planCode;
    @NotBlank(message = "计划名称不能为空")
    private String planName;
    private Long deviceId;
    private Long categoryId;
    private String cycleType;
    @NotEmpty(message = "点检项目不能为空")
    private List<String> checkItems;
    private Boolean enabled;
    private String remark;
}
