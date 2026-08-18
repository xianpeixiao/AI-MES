package com.aimes.dto.request.process;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessMaterialItem {
    private Long materialId;
    private BigDecimal qty;
    private String unit;
    private String materialType;
    private String remark;
}
