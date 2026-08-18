package com.aimes.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "姓名不能为空")
    private String realName;
    private String avatar;
}
