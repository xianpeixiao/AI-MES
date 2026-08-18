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
public class DashboardAlertsVo {

    private List<AlertExceptionVo> exceptions;
    private List<AlertMaterialVo> materials;
    private List<Map<String, Object>> devices;
}
