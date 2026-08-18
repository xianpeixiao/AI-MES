package com.aimes.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthVo {

    private String token;
    private String tokenType;
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String role;
    private Long teamId;
    private String teamName;
    private Integer status;
    private List<String> permissions;
    private Boolean fullAccess;
}
