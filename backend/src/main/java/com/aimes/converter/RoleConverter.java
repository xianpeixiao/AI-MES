package com.aimes.converter;

import com.aimes.vo.admin.RoleVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleConverter {

    public RoleVo toVo(String roleKey, String roleName, List<String> permissions, boolean fullAccess) {
        return RoleVo.builder()
                .id(roleKey)
                .roleName(roleName)
                .permissions(permissions)
                .fullAccess(fullAccess)
                .build();
    }
}
