package com.aimes.dto.request.deviceops;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DeviceMaintenancePlanSaveRequest {
    private String planCode;
    @NotBlank(message = "计划名称不能为空")
    private String planName;
    private Long deviceId;
    private Long categoryId;
    private String cycleType;
    @NotEmpty(message = "保养项目不能为空")
    private List<String> maintenanceItems;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;
    private Boolean enabled;
    private String remark;
}
