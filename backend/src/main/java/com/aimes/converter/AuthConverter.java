package com.aimes.converter;

import com.aimes.entity.ProdTeam;
import com.aimes.entity.SysUser;
import com.aimes.vo.auth.AuthVo;
import com.aimes.vo.auth.CaptchaRequiredVo;
import com.aimes.vo.auth.CaptchaVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthConverter {

    public AuthVo toVo(SysUser user, ProdTeam team, String token, List<String> permissions, boolean fullAccess) {
        if (user == null) {
            return null;
        }
        return AuthVo.builder()
                .token(token)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .teamName(team == null ? null : team.getTeamName())
                .status(user.getStatus())
                .permissions(permissions)
                .fullAccess(fullAccess)
                .build();
    }

    public CaptchaVo toCaptchaVo(Map<String, Object> captcha) {
        if (captcha == null) {
            return null;
        }
        return CaptchaVo.builder()
                .id((String) captcha.get("id"))
                .img((String) captcha.get("img"))
                .build();
    }

    public CaptchaRequiredVo toCaptchaRequiredVo(boolean required) {
        return CaptchaRequiredVo.builder().required(required).build();
    }
}
