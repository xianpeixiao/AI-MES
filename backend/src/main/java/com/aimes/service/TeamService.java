package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.converter.TeamConverter;
import com.aimes.dto.request.team.TeamSaveRequest;
import com.aimes.entity.ProdTeam;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.team.TeamDetailVo;
import com.aimes.vo.team.TeamMemberVo;
import com.aimes.vo.team.TeamTaskSummaryVo;
import com.aimes.vo.team.TeamTaskVo;
import com.aimes.vo.team.TeamVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final SysUserMapper sysUserMapper;
    private final ProdTeamMapper prodTeamMapper;
    private final ProdWorkOrderMapper prodWorkOrderMapper;
    private final ReferentialIntegrityService referentialIntegrityService;
    private final TeamConverter teamConverter;

    public List<TeamVo> list() {
        return prodTeamMapper.selectList(new LambdaQueryWrapper<ProdTeam>().orderByAsc(ProdTeam::getId))
                .stream()
                .map(this::toTeamVo)
                .toList();
    }

    public TeamDetailVo detail(Long id) {
        ProdTeam team = getTeam(id);
        TeamVo teamVo = toTeamVo(team);
        List<SysUser> members = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTeamId, id)
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        List<TeamTaskVo> tasks = prodWorkOrderMapper.selectList(new LambdaQueryWrapper<ProdWorkOrder>()
                        .eq(ProdWorkOrder::getTeamId, id)
                        .orderByAsc(ProdWorkOrder::getPriority)
                        .orderByDesc(ProdWorkOrder::getDeadline))
                .stream()
                .map(teamConverter::toTaskVo)
                .toList();
        List<TeamMemberVo> memberVos = members.stream().map(teamConverter::toMemberVo).toList();
        TeamTaskSummaryVo summary = teamConverter.toTaskSummary(tasks);
        return teamConverter.toDetailVo(teamVo, memberVos, tasks, summary);
    }

    @Transactional
    public TeamDetailVo create(TeamSaveRequest request) {
        ProdTeam team = new ProdTeam();
        team.setTeamCode(StringUtils.hasText(request.getTeamCode()) ? request.getTeamCode() : nextTeamCode());
        team.setTeamName(request.getTeamName());
        team.setLeaderId(request.getLeaderId());
        team.setMemberCount(request.getMemberCount() == null ? 0 : request.getMemberCount());
        team.setLineName(request.getLineName());
        team.setCreatedTime(LocalDateTime.now());
        prodTeamMapper.insert(team);
        return detail(team.getId());
    }

    @Transactional
    public TeamDetailVo update(Long id, TeamSaveRequest request) {
        ProdTeam team = getTeam(id);
        team.setTeamName(request.getTeamName());
        team.setLeaderId(request.getLeaderId());
        if (request.getMemberCount() != null) {
            team.setMemberCount(request.getMemberCount());
        }
        if (request.getLineName() != null) {
            team.setLineName(request.getLineName());
        }
        prodTeamMapper.updateById(team);
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        referentialIntegrityService.ensureTeamDeletable(id);
        prodTeamMapper.deleteById(id);
    }

    private ProdTeam getTeam(Long id) {
        ProdTeam team = prodTeamMapper.selectById(id);
        if (team == null) {
            throw new BusinessException("班组不存在");
        }
        return team;
    }

    private TeamVo toTeamVo(ProdTeam team) {
        SysUser leader = team.getLeaderId() == null ? null : sysUserMapper.selectById(team.getLeaderId());
        long activeOrders = prodWorkOrderMapper.selectCount(new LambdaQueryWrapper<ProdWorkOrder>()
                .eq(ProdWorkOrder::getTeamId, team.getId())
                .in(ProdWorkOrder::getStatus, List.of("assigned", "producing", "exception")));
        return teamConverter.toVo(team, leader, activeOrders);
    }

    private String nextTeamCode() {
        long count = prodTeamMapper.selectCount(null) + 1;
        return "T-" + String.format("%03d", count);
    }
}
