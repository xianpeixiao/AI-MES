package com.aimes.dto.request.coze;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CozeSchedulingRequest {
    private LocalDate planDate;
    @NotEmpty(message = "工单列表不能为空")
    private List<Long> workOrderIds;
    private Boolean materialConstraint;
    private Boolean deviceConstraint;
    private Boolean teamConstraint;
}
