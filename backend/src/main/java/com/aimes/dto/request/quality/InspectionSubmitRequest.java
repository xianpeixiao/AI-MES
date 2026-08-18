package com.aimes.dto.request.quality;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InspectionSubmitRequest {
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;
    @NotBlank(message = "工序名称不能为空")
    private String processName;
    @NotEmpty(message = "检验项不能为空")
    private List<InspectionItemRequest> items;
}
