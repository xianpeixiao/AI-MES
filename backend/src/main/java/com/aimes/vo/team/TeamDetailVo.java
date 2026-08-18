package com.aimes.vo.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TeamDetailVo extends TeamVo {

    private List<TeamMemberVo> members;
    private List<TeamTaskVo> tasks;
    private TeamTaskSummaryVo taskSummary;
}
