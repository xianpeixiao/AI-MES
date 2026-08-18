package com.aimes.vo.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTaskSummaryVo {

    private long pending;
    private long producing;
    private long done;
}
