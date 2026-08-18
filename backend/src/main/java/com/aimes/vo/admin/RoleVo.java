package com.aimes.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVo {

    private String id;
    private String roleName;
    private List<String> permissions;
    private Boolean fullAccess;
}
