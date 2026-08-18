package com.aimes.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamProgressVo {

    private Long teamId;
    private String teamCode;
    private String teamName;
    private String lineName;
    private Integer avgProgress;
    private Long producingCount;
    private Long doneCount;
    private Long totalCount;
}
