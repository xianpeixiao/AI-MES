package com.aimes.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVo {

    private long planCount;
    private String planTrend;
    private long inProgressWorkOrderCount;
    private String inProgressTrend;
    private long openExceptionCount;
    private long newExceptionCount;
    private long materialAlertCount;
    private long newMaterialAlertCount;
    private List<Map<String, Object>> outputTrend;
    private long todayOutput;
    private Long deviceTotalCount;
    private Long deviceRunningCount;
    private Long deviceFaultCount;
    private Long deviceTodayAlertCount;
    private Long deviceMaintenanceOverdueCount;
    private Object deviceAvgUtilizationRate;
}
