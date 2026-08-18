package com.aimes.vo.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTaskVo {

    private Long workOrderId;
    private String orderNo;
    private String productName;
    private String processName;
    private String status;
    private Integer progress;
}
