package com.aimes.converter;

import com.aimes.entity.MatMaterial;
import com.aimes.vo.material.MaterialListVo;
import com.aimes.vo.material.MaterialSummaryVo;
import com.aimes.vo.material.MaterialVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MaterialConverter {

    public MaterialVo toVo(MatMaterial material) {
        if (material == null) {
            return null;
        }
        BigDecimal gap = material.getSafetyStock().subtract(material.getStockQty());
        if (gap.compareTo(BigDecimal.ZERO) < 0) {
            gap = BigDecimal.ZERO;
        }
        return MaterialVo.builder()
                .id(material.getId())
                .materialCode(material.getMaterialCode())
                .materialName(material.getMaterialName())
                .stockQty(material.getStockQty())
                .safetyStock(material.getSafetyStock())
                .gap(gap)
                .unit(material.getUnit())
                .alertStatus(material.getAlertStatus())
                .remark(material.getRemark())
                .updatedTime(material.getUpdatedTime())
                .build();
    }

    public MaterialListVo toListVo(List<MaterialVo> records) {
        long warningCount = records.stream().filter(item -> "warning".equals(item.getAlertStatus())).count();
        MaterialSummaryVo summary = MaterialSummaryVo.builder()
                .total(records.size())
                .normal(records.size() - warningCount)
                .warning(warningCount)
                .build();
        return MaterialListVo.builder()
                .summary(summary)
                .records(records)
                .build();
    }
}
