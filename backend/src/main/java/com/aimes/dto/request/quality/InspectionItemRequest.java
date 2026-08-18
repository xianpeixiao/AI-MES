package com.aimes.dto.request.quality;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InspectionItemRequest {
    private Long planId;
    @NotBlank(message = "检验项名称不能为空")
    private String itemName;
    private String measuredValue;
    private String result;
    private String remark;
}
