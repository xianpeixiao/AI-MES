package com.aimes.dto.request.coze;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SchedulingApplyRequest {
    private LocalDate planDate;
    private String summary;
    private List<SchedulingPriorityItem> priorities;
    private List<SchedulingBottleneckItem> bottlenecks;
    @NotEmpty(message = "派工建议不能为空")
    private List<SchedulingDispatchItem> dispatches;
}
