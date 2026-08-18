package com.aimes.dto.request.material;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaterialCreateRequest {
    private String materialCode;
    @NotBlank(message = "物料名称不能为空")
    private String materialName;
    @Min(value = 0, message = "当前库存不能小于 0")
    private Double stockQty;
    @NotNull(message = "安全库存不能为空")
    @Min(value = 0, message = "安全库存不能小于 0")
    private Double safetyStock;
    private String unit;
    private String remark;
}
