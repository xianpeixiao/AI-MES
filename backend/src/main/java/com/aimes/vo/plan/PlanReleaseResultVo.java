package com.aimes.vo.plan;

import com.aimes.vo.workorder.WorkOrderSummaryVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanReleaseResultVo {

    private PlanVo plan;
    private List<WorkOrderSummaryVo> generatedWorkOrders;
    private WorkOrderSummaryVo generatedWorkOrder;
    private Boolean hasBom;
}
