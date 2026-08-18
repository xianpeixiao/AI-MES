package com.aimes.dto.request.coze;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CozeChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;
    private String sessionId;
}
