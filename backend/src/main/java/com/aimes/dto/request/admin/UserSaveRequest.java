package com.aimes.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSaveRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String password;
    @NotBlank(message = "姓名不能为空")
    private String realName;
    @NotBlank(message = "角色不能为空")
    private String role;
    private Long teamId;
    @NotNull(message = "状态不能为空")
    private Integer status;
}
