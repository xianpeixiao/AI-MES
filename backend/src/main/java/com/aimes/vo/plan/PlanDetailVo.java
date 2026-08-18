package com.aimes.vo.plan;

import com.aimes.vo.product.ProductVo;
import com.aimes.vo.workorder.WorkOrderSummaryVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlanDetailVo extends PlanVo {

    private List<WorkOrderSummaryVo> workOrders;
    private Integer completionProgress;
    private String executionStatus;
    private ProductVo product;
    private Boolean hasBom;
}
