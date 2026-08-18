package com.aimes.dto.request.workorder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkOrderProgressRequest {
    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能小于 0")
    @Max(value = 100, message = "进度不能大于 100")
    private Integer progress;
    @NotBlank(message = "当前工序不能为空")
    private String processName;
    private Boolean completeCurrentProcess;
    private Long deviceId;
    private List<Long> deviceIds;
    private String remark;
}
