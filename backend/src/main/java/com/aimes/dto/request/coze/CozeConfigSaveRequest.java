package com.aimes.dto.request.coze;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CozeConfigSaveRequest {
    private String apiToken;
    @NotBlank(message = "Bot ID 不能为空")
    private String botId;
    @NotBlank(message = "API 地址不能为空")
    private String apiUrl;
    private String workflowId;
    private String welcomeMessage;
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
