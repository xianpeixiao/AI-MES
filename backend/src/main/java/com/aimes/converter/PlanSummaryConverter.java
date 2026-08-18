package com.aimes.converter;

import com.aimes.vo.plan.PlanBriefVo;
import com.aimes.vo.plan.PlanSummaryVo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlanSummaryConverter {

    public Map<String, Object> toMap(PlanSummaryVo summary) {
        if (summary == null) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalCount", summary.getTotalCount());
        map.put("draftCount", summary.getDraftCount());
        map.put("releasedCount", summary.getReleasedCount());
        map.put("completedCount", summary.getCompletedCount());
        map.put("pausedCount", summary.getPausedCount());
        map.put("plans", summary.getPlans().stream().map(this::briefToMap).toList());
        return map;
    }

    public List<Map<String, Object>> briefListToMap(List<PlanBriefVo> plans) {
        return plans.stream().map(this::briefToMap).toList();
    }

    public Map<String, Object> briefToMap(PlanBriefVo plan) {
        if (plan == null) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plan.getId());
        map.put("planNo", plan.getPlanNo());
        map.put("productName", plan.getProductName());
        map.put("planQty", plan.getPlanQty());
        map.put("planDate", plan.getPlanDate());
        map.put("status", plan.getStatus());
        map.put("statusLabel", plan.getStatusLabel());
        map.put("releaseTime", plan.getReleaseTime());
        return map;
    }
}
