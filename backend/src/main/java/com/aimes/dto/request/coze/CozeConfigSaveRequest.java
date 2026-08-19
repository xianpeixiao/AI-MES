package com.aimes.dto.request.coze;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CozeConfigSaveRequest {
    /** coze | deepseek | auto */
    private String aiProvider;
    private String apiToken;
    private String botId;
    private String apiUrl;
    private String workflowId;
    private String welcomeMessage;
    private String deepseekApiKey;
    private String deepseekApiUrl;
    private String deepseekModel;
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
