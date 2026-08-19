package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.dto.request.coze.CozeConfigSaveRequest;
import com.aimes.entity.SysCozeConfig;
import com.aimes.mapper.SysCozeConfigMapper;
import com.aimes.service.ai.AiProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CozeConfigService {

    private static final long CONFIG_ID = 1L;

    private final SysCozeConfigMapper sysCozeConfigMapper;

    @Value("${coze.api-token:}")
    private String envApiToken;

    @Value("${coze.bot-id:}")
    private String envBotId;

    @Value("${coze.api-url:https://api.coze.cn/v3}")
    private String envApiUrl;

    @Value("${coze.workflow-id:}")
    private String envWorkflowId;

    @Value("${deepseek.api-key:}")
    private String envDeepSeekApiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com}")
    private String envDeepSeekApiUrl;

    @Value("${deepseek.model:deepseek-v4-flash}")
    private String envDeepSeekModel;

    @jakarta.annotation.PostConstruct
    public void syncFromEnvIfEmpty() {
        SysCozeConfig row = loadRow();
        boolean changed = false;
        if (!StringUtils.hasText(row.getApiToken()) && StringUtils.hasText(envApiToken)) {
            row.setApiToken(envApiToken.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getBotId()) && StringUtils.hasText(envBotId)) {
            row.setBotId(envBotId.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getApiUrl()) && StringUtils.hasText(envApiUrl)) {
            row.setApiUrl(envApiUrl.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getWorkflowId()) && StringUtils.hasText(envWorkflowId)) {
            row.setWorkflowId(envWorkflowId.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getDeepseekApiKey()) && StringUtils.hasText(envDeepSeekApiKey)) {
            row.setDeepseekApiKey(envDeepSeekApiKey.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getDeepseekApiUrl()) && StringUtils.hasText(envDeepSeekApiUrl)) {
            row.setDeepseekApiUrl(envDeepSeekApiUrl.trim());
            changed = true;
        }
        if (!StringUtils.hasText(row.getDeepseekModel()) && StringUtils.hasText(envDeepSeekModel)) {
            row.setDeepseekModel(envDeepSeekModel.trim());
            changed = true;
        }
        if (changed) {
            row.setUpdateTime(LocalDateTime.now());
            sysCozeConfigMapper.updateById(row);
        }
    }

    public AiProviderType getConfiguredProvider() {
        SysCozeConfig row = loadRow();
        return AiProviderType.fromWireValue(row.getAiProvider());
    }

    public AiProviderType resolveActiveProvider() {
        AiProviderType configured = getConfiguredProvider();
        if (configured == AiProviderType.COZE) {
            return AiProviderType.COZE;
        }
        if (configured == AiProviderType.DEEPSEEK) {
            return AiProviderType.DEEPSEEK;
        }
        if (isCozeConfigured()) {
            return AiProviderType.COZE;
        }
        if (isDeepSeekConfigured()) {
            return AiProviderType.DEEPSEEK;
        }
        return AiProviderType.COZE;
    }

    public String getEffectiveApiToken() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getApiToken())) {
            return row.getApiToken().trim();
        }
        return StringUtils.hasText(envApiToken) ? envApiToken.trim() : "";
    }

    public String getEffectiveBotId() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getBotId())) {
            return row.getBotId().trim();
        }
        return StringUtils.hasText(envBotId) ? envBotId.trim() : "";
    }

    public String getEffectiveApiUrl() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getApiUrl())) {
            return row.getApiUrl().trim();
        }
        return StringUtils.hasText(envApiUrl) ? envApiUrl.trim() : "https://api.coze.cn/v3";
    }

    public String getEffectiveWorkflowId() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getWorkflowId())) {
            return row.getWorkflowId().trim();
        }
        return StringUtils.hasText(envWorkflowId) ? envWorkflowId.trim() : "";
    }

    public String getEffectiveDeepSeekApiKey() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getDeepseekApiKey())) {
            return row.getDeepseekApiKey().trim();
        }
        return StringUtils.hasText(envDeepSeekApiKey) ? envDeepSeekApiKey.trim() : "";
    }

    public String getEffectiveDeepSeekApiUrl() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getDeepseekApiUrl())) {
            return row.getDeepseekApiUrl().trim();
        }
        return StringUtils.hasText(envDeepSeekApiUrl) ? envDeepSeekApiUrl.trim() : "https://api.deepseek.com";
    }

    public String getEffectiveDeepSeekModel() {
        SysCozeConfig row = loadRow();
        if (StringUtils.hasText(row.getDeepseekModel())) {
            return row.getDeepseekModel().trim();
        }
        if (StringUtils.hasText(envDeepSeekModel)) {
            return envDeepSeekModel.trim();
        }
        return "deepseek-chat";
    }

    public String getWelcomeMessage() {
        SysCozeConfig row = loadRow();
        return StringUtils.hasText(row.getWelcomeMessage())
                ? row.getWelcomeMessage()
                : "您好，我是 AI-MES 智能助手，可协助查询工单进度、异常处理及 SOP 指导。";
    }

    public boolean isEnabled() {
        SysCozeConfig row = loadRow();
        return row.getEnabled() == null || row.getEnabled() == 1;
    }

    public boolean isCozeConfigured() {
        return isEnabled()
                && StringUtils.hasText(getEffectiveApiToken())
                && StringUtils.hasText(getEffectiveBotId());
    }

    /** 兼容旧调用 */
    public boolean isConfigured() {
        return isCozeConfigured();
    }

    public boolean isDeepSeekConfigured() {
        return isEnabled() && StringUtils.hasText(getEffectiveDeepSeekApiKey());
    }

    public boolean isActiveEngineConfigured() {
        AiProviderType active = resolveActiveProvider();
        if (active == AiProviderType.DEEPSEEK) {
            return isDeepSeekConfigured();
        }
        return isCozeConfigured();
    }

    public Map<String, Object> getConfigView() {
        SysCozeConfig row = loadRow();
        String effectiveToken = getEffectiveApiToken();
        String effectiveBotId = getEffectiveBotId();
        String effectiveApiUrl = getEffectiveApiUrl();
        String effectiveDeepSeekKey = getEffectiveDeepSeekApiKey();
        AiProviderType provider = getConfiguredProvider();
        AiProviderType activeProvider = resolveActiveProvider();

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("aiProvider", provider.wireValue());
        view.put("activeProvider", activeProvider.wireValue());
        view.put("botId", effectiveBotId);
        view.put("apiUrl", effectiveApiUrl);
        view.put("workflowId", StringUtils.hasText(row.getWorkflowId()) ? row.getWorkflowId() : "");
        view.put("welcomeMessage", getWelcomeMessage());
        view.put("enabled", isEnabled());
        view.put("hasApiToken", StringUtils.hasText(effectiveToken));
        view.put("apiTokenMasked", maskToken(effectiveToken));
        view.put("configured", isCozeConfigured());
        view.put("cozeConfigured", isCozeConfigured());
        view.put("deepseekConfigured", isDeepSeekConfigured());
        view.put("activeConfigured", isActiveEngineConfigured());
        view.put("deepseekApiUrl", getEffectiveDeepSeekApiUrl());
        view.put("deepseekModel", getEffectiveDeepSeekModel());
        view.put("hasDeepseekApiKey", StringUtils.hasText(effectiveDeepSeekKey));
        view.put("deepseekApiKeyMasked", maskToken(effectiveDeepSeekKey));
        view.put("tokenSource", StringUtils.hasText(row.getApiToken()) ? "database" : (StringUtils.hasText(envApiToken) ? "env" : "none"));
        view.put("botSource", StringUtils.hasText(row.getBotId()) ? "database" : (StringUtils.hasText(envBotId) ? "env" : "none"));
        view.put("deepseekKeySource", StringUtils.hasText(row.getDeepseekApiKey()) ? "database" : (StringUtils.hasText(envDeepSeekApiKey) ? "env" : "none"));
        view.put("updateTime", row.getUpdateTime());
        return view;
    }

    public Map<String, Object> saveConfig(CozeConfigSaveRequest request) {
        AiProviderType provider = AiProviderType.fromWireValue(request.getAiProvider());
        validateSaveRequest(request, provider);

        SysCozeConfig row = loadRow();
        row.setAiProvider(provider.wireValue());
        if (StringUtils.hasText(request.getApiToken()) && !request.getApiToken().contains("*")) {
            row.setApiToken(request.getApiToken().trim());
        }
        row.setBotId(StringUtils.hasText(request.getBotId()) ? request.getBotId().trim() : null);
        row.setApiUrl(StringUtils.hasText(request.getApiUrl()) ? request.getApiUrl().trim() : "https://api.coze.cn/v3");
        row.setWorkflowId(StringUtils.hasText(request.getWorkflowId()) ? request.getWorkflowId().trim() : null);
        row.setWelcomeMessage(StringUtils.hasText(request.getWelcomeMessage()) ? request.getWelcomeMessage().trim() : null);
        if (StringUtils.hasText(request.getDeepseekApiKey()) && !request.getDeepseekApiKey().contains("*")) {
            row.setDeepseekApiKey(request.getDeepseekApiKey().trim());
        }
        row.setDeepseekApiUrl(StringUtils.hasText(request.getDeepseekApiUrl())
                ? request.getDeepseekApiUrl().trim()
                : "https://api.deepseek.com");
        row.setDeepseekModel(StringUtils.hasText(request.getDeepseekModel())
                ? request.getDeepseekModel().trim()
                : "deepseek-chat");
        row.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        row.setUpdateTime(LocalDateTime.now());
        sysCozeConfigMapper.updateById(row);
        return getConfigView();
    }

    private void validateSaveRequest(CozeConfigSaveRequest request, AiProviderType provider) {
        boolean hasCozeToken = StringUtils.hasText(request.getApiToken()) && !request.getApiToken().contains("*")
                || StringUtils.hasText(getEffectiveApiToken());
        boolean hasDeepSeekKey = StringUtils.hasText(request.getDeepseekApiKey()) && !request.getDeepseekApiKey().contains("*")
                || StringUtils.hasText(getEffectiveDeepSeekApiKey());

        if (provider == AiProviderType.COZE) {
            if (!StringUtils.hasText(request.getBotId())) {
                throw new BusinessException("请填写 Coze Bot ID");
            }
            if (!hasCozeToken) {
                throw new BusinessException("请填写 Coze API Token");
            }
            if (!StringUtils.hasText(request.getApiUrl())) {
                throw new BusinessException("请填写 Coze API 地址");
            }
            return;
        }
        if (provider == AiProviderType.DEEPSEEK) {
            if (!hasDeepSeekKey) {
                throw new BusinessException("请填写 DeepSeek API Key");
            }
            if (!StringUtils.hasText(request.getDeepseekApiUrl())) {
                throw new BusinessException("请填写 DeepSeek API 地址");
            }
            if (!StringUtils.hasText(request.getDeepseekModel())) {
                throw new BusinessException("请填写 DeepSeek 模型名称");
            }
            return;
        }
        if (!hasCozeToken && !hasDeepSeekKey) {
            throw new BusinessException("自动模式下请至少配置 Coze 或 DeepSeek 其中一套凭证");
        }
    }

    private SysCozeConfig loadRow() {
        SysCozeConfig row = sysCozeConfigMapper.selectById(CONFIG_ID);
        if (row != null) {
            ensureDefaults(row);
            return row;
        }
        row = new SysCozeConfig();
        row.setId(CONFIG_ID);
        row.setApiUrl("https://api.coze.cn/v3");
        row.setEnabled(1);
        row.setAiProvider(AiProviderType.COZE.wireValue());
        row.setDeepseekApiUrl("https://api.deepseek.com");
        row.setDeepseekModel("deepseek-v4-flash");
        row.setUpdateTime(LocalDateTime.now());
        sysCozeConfigMapper.insert(row);
        return row;
    }

    private void ensureDefaults(SysCozeConfig row) {
        boolean changed = false;
        if (!StringUtils.hasText(row.getAiProvider())) {
            row.setAiProvider(AiProviderType.COZE.wireValue());
            changed = true;
        }
        if (!StringUtils.hasText(row.getDeepseekApiUrl())) {
            row.setDeepseekApiUrl("https://api.deepseek.com");
            changed = true;
        }
        if (!StringUtils.hasText(row.getDeepseekModel()) || "deepseek-chat".equalsIgnoreCase(row.getDeepseekModel())) {
            row.setDeepseekModel("deepseek-v4-flash");
            changed = true;
        }
        if (changed) {
            row.setUpdateTime(LocalDateTime.now());
            sysCozeConfigMapper.updateById(row);
        }
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        if (token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
