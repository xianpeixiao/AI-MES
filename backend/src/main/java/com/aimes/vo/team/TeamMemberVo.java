package com.aimes.vo.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberVo {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private Integer status;
}
