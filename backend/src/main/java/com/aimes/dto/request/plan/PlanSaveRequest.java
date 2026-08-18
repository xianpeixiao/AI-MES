package com.aimes.dto.request.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanSaveRequest {
    private String planNo;
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    private Long productId;
    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量必须大于 0")
    private Integer planQty;
    @NotNull(message = "计划日期不能为空")
    private LocalDate planDate;
    private String status;
    private String remark;
}
