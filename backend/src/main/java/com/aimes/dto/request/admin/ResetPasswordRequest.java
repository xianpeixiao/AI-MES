package com.aimes.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6-64 位")
    private String password;
}
