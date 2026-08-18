package com.aimes.service;

import cn.dev33.satoken.stp.StpUtil;
import com.aimes.common.BusinessException;
import com.aimes.common.OperationLog;
import com.aimes.common.OperationLogRunner;
import com.aimes.converter.AuthConverter;
import com.aimes.dto.request.auth.LoginRequest;
import com.aimes.dto.request.auth.PasswordChangeRequest;
import com.aimes.dto.request.auth.ProfileUpdateRequest;
import com.aimes.entity.ProdTeam;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.security.CaptchaService;
import com.aimes.security.LoginProtectionService;
import com.aimes.util.ClientIpUtil;
import com.aimes.vo.auth.AuthVo;
import com.aimes.vo.auth.CaptchaRequiredVo;
import com.aimes.vo.auth.CaptchaVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final ProdTeamMapper prodTeamMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final LoginProtectionService loginProtectionService;
    private final RoleService roleService;
    private final OperationLogRunner operationLogRunner;
    private final AuthConverter authConverter;

    @OperationLog(module = "认证", action = "登录")
    public AuthVo login(LoginRequest request) {
        return operationLogRunner.runUnchecked("认证", "登录", "login", new Object[]{request}, () -> loginInternal(request));
    }

    private AuthVo loginInternal(LoginRequest request) {
        String ip = ClientIpUtil.current();
        String username = request.getUsername().trim();
        loginProtectionService.checkAllowed(ip, username);
        if (loginProtectionService.captchaRequired(ip)) {
            captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer());
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginProtectionService.recordFailure(ip, username);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        loginProtectionService.clearOnSuccess(ip, username);
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());
        return buildAuthPayload(user);
    }

    public void logout() {
        StpUtil.logout();
    }

    public AuthVo info() {
        return buildAuthPayload(currentUser());
    }

    public CaptchaVo captcha() {
        return authConverter.toCaptchaVo(captchaService.create());
    }

    public CaptchaRequiredVo captchaRequired() {
        return authConverter.toCaptchaRequiredVo(loginProtectionService.captchaRequired(ClientIpUtil.current()));
    }

    public SysUser currentUser() {
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(401, "用户不存在或登录已失效");
        }
        return user;
    }

    private AuthVo buildAuthPayload(SysUser user) {
        ProdTeam team = user.getTeamId() == null ? null : prodTeamMapper.selectById(user.getTeamId());
        return authConverter.toVo(
                user,
                team,
                StpUtil.getTokenValue(),
                roleService.getPermissionsByRoleKey(user.getRole()),
                roleService.hasFullAccess(user.getRole())
        );
    }

    public AuthVo updateProfile(ProfileUpdateRequest request) {
        SysUser user = currentUser();
        user.setRealName(request.getRealName());
        user.setAvatar(request.getAvatar());
        sysUserMapper.updateById(user);
        return buildAuthPayload(user);
    }

    public void changePassword(PasswordChangeRequest request) {
        SysUser user = currentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }
}
