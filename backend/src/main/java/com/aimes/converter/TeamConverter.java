package com.aimes.converter;

import com.aimes.entity.ProdTeam;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.vo.team.TeamDetailVo;
import com.aimes.vo.team.TeamMemberVo;
import com.aimes.vo.team.TeamTaskSummaryVo;
import com.aimes.vo.team.TeamTaskVo;
import com.aimes.vo.team.TeamVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamConverter {

    public TeamVo toVo(ProdTeam team, SysUser leader, long activeOrderCount) {
        if (team == null) {
            return null;
        }
        return TeamVo.builder()
                .id(team.getId())
                .teamCode(team.getTeamCode())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .leaderName(leader == null ? null : leader.getRealName())
                .memberCount(team.getMemberCount())
                .lineName(team.getLineName())
                .activeOrderCount(activeOrderCount)
                .createdTime(team.getCreatedTime())
                .updatedTime(team.getUpdatedTime())
                .build();
    }

    public TeamMemberVo toMemberVo(SysUser member) {
        if (member == null) {
            return null;
        }
        return TeamMemberVo.builder()
                .id(member.getId())
                .username(member.getUsername())
                .realName(member.getRealName())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }

    public TeamTaskVo toTaskVo(ProdWorkOrder order) {
        if (order == null) {
            return null;
        }
        return TeamTaskVo.builder()
                .workOrderId(order.getId())
                .orderNo(order.getOrderNo())
                .productName(order.getProductName())
                .processName(order.getProcessName())
                .status(order.getStatus())
                .progress(order.getProgress() == null ? 0 : order.getProgress())
                .build();
    }

    public TeamTaskSummaryVo toTaskSummary(List<TeamTaskVo> tasks) {
        return TeamTaskSummaryVo.builder()
                .pending(tasks.stream().filter(t -> "assigned".equals(t.getStatus()) || "pending".equals(t.getStatus())).count())
                .producing(tasks.stream().filter(t -> "producing".equals(t.getStatus())).count())
                .done(tasks.stream().filter(t -> "done".equals(t.getStatus())).count())
                .build();
    }

    public TeamDetailVo toDetailVo(TeamVo team, List<TeamMemberVo> members, List<TeamTaskVo> tasks, TeamTaskSummaryVo summary) {
        if (team == null) {
            return null;
        }
        return TeamDetailVo.builder()
                .id(team.getId())
                .teamCode(team.getTeamCode())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .leaderName(team.getLeaderName())
                .memberCount(team.getMemberCount())
                .lineName(team.getLineName())
                .activeOrderCount(team.getActiveOrderCount())
                .createdTime(team.getCreatedTime())
                .updatedTime(team.getUpdatedTime())
                .members(members)
                .tasks(tasks)
                .taskSummary(summary)
                .build();
    }
}
