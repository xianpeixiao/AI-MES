package com.aimes.dto.request.product;

import lombok.Data;

@Data
public class BomItemRequest {
    private Long materialId;
    private Double qty;
    private String unit;
    private Double lossRate;
    private String remark;
}
