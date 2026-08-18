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
public class PlanSummaryVo {

    private long totalCount;
    private long draftCount;
    private long releasedCount;
    private long completedCount;
    private long pausedCount;
    private List<PlanBriefVo> plans;
}
