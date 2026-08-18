package com.aimes.dto.request.material;

import lombok.Data;

@Data
public class MaterialUpdateRequest {
    private Double stockQty;
    private Double inboundQty;
    private String materialName;
    private Double safetyStock;
    private String unit;
    private String remark;
}
