package com.aimes.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMaterialVo {

    private Long id;
    private String materialCode;
    private String materialName;
    private BigDecimal stockQty;
    private BigDecimal safetyStock;
    private BigDecimal gap;
    private String unit;
    private String status;
}
