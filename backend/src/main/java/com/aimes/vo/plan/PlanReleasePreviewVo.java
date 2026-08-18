package com.aimes.vo.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanReleasePreviewVo {

    private PlanVo plan;
    private Integer splitQty;
    private Integer workOrderCount;
    private List<PlanSplitPreviewItemVo> workOrders;
    private Boolean hasBom;
    private String bomWarning;
}
