package com.aimes.dto.request.workorder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderAssignRequest {
    @NotNull(message = "班组不能为空")
    private Long teamId;
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小为 1")
    @Max(value = 3, message = "优先级最大为 3")
    private Integer priority;
    private String remark;
}
