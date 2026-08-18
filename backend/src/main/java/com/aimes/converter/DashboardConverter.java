package com.aimes.converter;

import com.aimes.entity.DevDevice;
import com.aimes.entity.ExcEvent;
import com.aimes.entity.MatMaterial;
import com.aimes.entity.ProdTeam;
import com.aimes.vo.dashboard.AlertExceptionVo;
import com.aimes.vo.dashboard.AlertMaterialVo;
import com.aimes.vo.dashboard.DashboardAlertsVo;
import com.aimes.vo.dashboard.DashboardStatsVo;
import com.aimes.vo.dashboard.TeamProgressVo;
import com.aimes.vo.dashboard.WorkshopSummaryVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class DashboardConverter {

    public DashboardStatsVo toStatsVo(long planCount, String planTrend, long inProgressCount, String inProgressTrend,
                                      long openExceptionCount, long newExceptionCount, long materialAlertCount,
                                      long newMaterialAlertCount, List<Map<String, Object>> outputTrend, long todayOutput,
                                      Map<String, Object> deviceSummary) {
        return DashboardStatsVo.builder()
                .planCount(planCount)
                .planTrend(planTrend)
                .inProgressWorkOrderCount(inProgressCount)
                .inProgressTrend(inProgressTrend)
                .openExceptionCount(openExceptionCount)
                .newExceptionCount(newExceptionCount)
                .materialAlertCount(materialAlertCount)
                .newMaterialAlertCount(newMaterialAlertCount)
                .outputTrend(outputTrend)
                .todayOutput(todayOutput)
                .deviceTotalCount((Long) deviceSummary.get("totalCount"))
                .deviceRunningCount((Long) deviceSummary.get("runningCount"))
                .deviceFaultCount((Long) deviceSummary.get("faultCount"))
                .deviceTodayAlertCount((Long) deviceSummary.get("todayAlertCount"))
                .deviceMaintenanceOverdueCount((Long) deviceSummary.get("maintenanceOverdueCount"))
                .deviceAvgUtilizationRate(deviceSummary.get("avgUtilizationRate"))
                .build();
    }

    public TeamProgressVo toTeamProgressVo(ProdTeam team, int avgProgress, long producing, long done, long total) {
        return TeamProgressVo.builder()
                .teamId(team.getId())
                .teamCode(team.getTeamCode())
                .teamName(team.getTeamName())
                .lineName(team.getLineName())
                .avgProgress(avgProgress)
                .producingCount(producing)
                .doneCount(done)
                .totalCount(total)
                .build();
    }

    public AlertExceptionVo toAlertExceptionVo(ExcEvent event, DevDevice device) {
        return AlertExceptionVo.builder()
                .id(event.getId())
                .eventNo(event.getEventNo())
                .eventType(event.getEventType())
                .workOrderId(event.getWorkOrderId())
                .status(event.getStatus())
                .description(event.getDescription())
                .deviceId(event.getDeviceId())
                .deviceCode(device == null ? null : device.getDeviceCode())
                .deviceName(device == null ? null : device.getDeviceName())
                .occurTime(event.getOccurTime())
                .build();
    }

    public AlertMaterialVo toAlertMaterialVo(MatMaterial material) {
        BigDecimal gap = material.getSafetyStock().subtract(material.getStockQty()).max(BigDecimal.ZERO);
        return AlertMaterialVo.builder()
                .id(material.getId())
                .materialCode(material.getMaterialCode())
                .materialName(material.getMaterialName())
                .stockQty(material.getStockQty())
                .safetyStock(material.getSafetyStock())
                .gap(gap)
                .unit(material.getUnit())
                .status(material.getAlertStatus())
                .build();
    }

    public DashboardAlertsVo toAlertsVo(List<AlertExceptionVo> exceptions, List<AlertMaterialVo> materials,
                                          List<Map<String, Object>> devices) {
        return DashboardAlertsVo.builder()
                .exceptions(exceptions)
                .materials(materials)
                .devices(devices)
                .build();
    }

    public WorkshopSummaryVo toWorkshopSummaryVo(int completionRate, long completedOrders, long totalOrders,
                                                 String lineName, String shiftName, long todayOutput,
                                                 int onTimeRate, long activeLines) {
        return WorkshopSummaryVo.builder()
                .completionRate(completionRate)
                .completedOrders(completedOrders)
                .totalOrders(totalOrders)
                .lineName(lineName)
                .shiftName(shiftName)
                .todayOutput(todayOutput)
                .onTimeRate(onTimeRate)
                .activeLines(activeLines)
                .build();
    }
}
