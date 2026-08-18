package com.aimes.vo.material;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialVo {

    private Long id;
    private String materialCode;
    private String materialName;
    private BigDecimal stockQty;
    private BigDecimal safetyStock;
    private BigDecimal gap;
    private String unit;
    private String alertStatus;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
