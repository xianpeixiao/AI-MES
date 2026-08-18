package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.converter.UserConverter;
import com.aimes.dto.request.admin.UserSaveRequest;
import com.aimes.entity.ProdTeam;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.admin.UserVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final SysUserMapper sysUserMapper;
    private final ProdTeamMapper prodTeamMapper;
    private final PasswordEncoder passwordEncoder;
    private final ReferentialIntegrityService referentialIntegrityService;
    private final UserConverter userConverter;

    public List<UserVo> list() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public UserVo detail(Long id) {
        return toVo(requireUser(id));
    }

    @Transactional
    public UserVo create(UserSaveRequest request) {
        ensureUniqueUsername(request.getUsername(), null);
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456"));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setTeamId(request.getTeamId());
        user.setStatus(request.getStatus());
        sysUserMapper.insert(user);
        syncTeamMemberCount(request.getTeamId());
        return toVo(user);
    }

    @Transactional
    public UserVo update(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        ensureUniqueUsername(request.getUsername(), id);
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        Long oldTeamId = user.getTeamId();
        user.setTeamId(request.getTeamId());
        user.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        sysUserMapper.updateById(user);
        syncTeamMemberCount(oldTeamId);
        syncTeamMemberCount(user.getTeamId());
        return toVo(user);
    }

    @Transactional
    public Map<String, Object> resetPassword(Long id, String password) {
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(password));
        sysUserMapper.updateById(user);
        return Map.of("id", id);
    }

    @Transactional
    public UserVo toggleStatus(Long id) {
        SysUser user = requireUser(id);
        user.setStatus(user.getStatus() != null && user.getStatus() == 1 ? 0 : 1);
        sysUserMapper.updateById(user);
        return toVo(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = requireUser(id);
        referentialIntegrityService.ensureUserDeletable(id);
        Long teamId = user.getTeamId();
        sysUserMapper.deleteById(id);
        syncTeamMemberCount(teamId);
        prodTeamMapper.update(null, new LambdaUpdateWrapper<ProdTeam>()
                .set(ProdTeam::getLeaderId, null)
                .eq(ProdTeam::getLeaderId, id));
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void ensureUniqueUsername(String username, Long ignoreId) {
        SysUser existed = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(ignoreId != null, SysUser::getId, ignoreId)
                .last("limit 1"));
        if (existed != null) {
            throw new BusinessException("用户名已存在");
        }
    }

    private void syncTeamMemberCount(Long teamId) {
        if (teamId == null) {
            return;
        }
        ProdTeam team = prodTeamMapper.selectById(teamId);
        if (team == null) {
            return;
        }
        long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTeamId, teamId)
                .eq(SysUser::getStatus, 1));
        team.setMemberCount((int) count);
        prodTeamMapper.updateById(team);
    }

    private UserVo toVo(SysUser user) {
        ProdTeam team = user.getTeamId() == null ? null : prodTeamMapper.selectById(user.getTeamId());
        return userConverter.toVo(user, team);
    }
}
