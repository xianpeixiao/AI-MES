package com.aimes.converter;

import com.aimes.entity.ProdTeam;
import com.aimes.entity.SysUser;
import com.aimes.vo.admin.UserVo;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserVo toVo(SysUser user, ProdTeam team) {
        if (user == null) {
            return null;
        }
        return UserVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .teamName(team == null ? null : team.getTeamName())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
