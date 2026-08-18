package com.aimes.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductSaveRequest {
    private String productCode;
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    private String spec;
    private String unit;
    private String status;
    private String remark;
}
