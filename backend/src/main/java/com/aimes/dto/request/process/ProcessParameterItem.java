package com.aimes.dto.request.process;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProcessParameterItem {
    @NotBlank(message = "参数名称不能为空")
    private String paramName;
    private String paramValue;
    private String minValue;
    private String maxValue;
    private String unit;
}
