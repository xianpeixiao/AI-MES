package com.aimes.service.ai;

import com.aimes.common.BusinessException;
import com.aimes.dto.request.coze.CozeChatRequest;
import com.aimes.dto.request.coze.CozeSchedulingRequest;
import com.aimes.entity.AiChatLog;
import com.aimes.entity.MatMaterial;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.AiChatLogMapper;
import com.aimes.mapper.MatMaterialMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.service.AuthService;
import com.aimes.service.CozeConfigService;
import com.aimes.service.coze.CozeChatPromptMode;
import com.aimes.service.coze.CozeChatPromptService;
import com.aimes.service.coze.CozeConstants;
import com.aimes.service.coze.CozePromptLoader;
import com.aimes.service.coze.CozeSchedulingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeepSeekEngineService {

    private final AiChatLogMapper aiChatLogMapper;
    private final AuthService authService;
    private final CozeConfigService cozeConfigService;
    private final CozeChatPromptService cozeChatPromptService;
    private final CozeSchedulingService cozeSchedulingService;
    private final CozePromptLoader cozePromptLoader;
    private final DeepSeekApiClient deepSeekApiClient;
    private final ProdWorkOrderMapper prodWorkOrderMapper;
    private final MatMaterialMapper matMaterialMapper;
    private final ObjectMapper objectMapper;

    public Map<String, Object> chat(CozeChatRequest request) {
        SysUser user = authService.currentUser();
        String sessionId = StringUtils.hasText(request.getSessionId()) ? request.getSessionId() : UUID.randomUUID().toString();
        ChatContext context = buildChatContext(user, request.getMessage(), sessionId);

        String reply;
        String mode;
        try {
            if (deepSeekApiClient.isConfigured()) {
                reply = deepSeekApiClient.chatCompletion(
                        cozePromptLoader.botPersona(),
                        resolveDeepSeekUserPrompt(user, request.getMessage(), context));
                mode = "live-deepseek";
            } else {
                reply = cozeChatPromptService.buildMockReply(request.getMessage(), context.referencedOrders(), user);
                mode = "mock";
            }
        } catch (Exception ex) {
            reply = cozeChatPromptService.buildMockReply(request.getMessage(), context.referencedOrders(), user);
            if (StringUtils.hasText(reply)) {
                mode = deepSeekApiClient.isConfigured() ? "mock-fallback" : "mock";
            } else if (deepSeekApiClient.isConfigured()) {
                String detail = ex.getMessage() == null ? "" : ex.getMessage();
                if (detail.contains("timeout")) {
                    throw new BusinessException("AI 响应超时，请稍后重试");
                }
                throw new BusinessException("DeepSeek 对话失败：" + detail);
            } else {
                mode = "mock-fallback";
            }
        }

        if (!StringUtils.hasText(reply)) {
            reply = cozeChatPromptService.buildMockReply(request.getMessage(), context.referencedOrders(), user);
            mode = "mock-fallback";
        }

        saveChatLog(user.getId(), sessionId, request.getMessage(), reply);
        return Map.of(
                "reply", reply,
                "sessionId", sessionId,
                "mode", mode,
                "provider", AiProviderType.DEEPSEEK.wireValue(),
                "promptMode", context.promptMode().wireValue(),
                "contextOrders", context.referencedOrders().stream().map(ProdWorkOrder::getOrderNo).toList()
        );
    }

    public void chatStream(CozeChatRequest request, HttpServletResponse response) {
        SysUser user = authService.currentUser();
        String sessionId = StringUtils.hasText(request.getSessionId()) ? request.getSessionId() : UUID.randomUUID().toString();
        ChatContext context = buildChatContext(user, request.getMessage(), sessionId);

        PrintWriter writer;
        try {
            writer = response.getWriter();
        } catch (IOException e) {
            throw new BusinessException("获取输出流失败: " + e.getMessage());
        }

        if (!deepSeekApiClient.isConfigured()) {
            String reply = cozeChatPromptService.buildMockReply(request.getMessage(), context.referencedOrders(), user);
            streamMockReply(writer, reply, sessionId, context.promptMode().wireValue());
            saveChatLog(user.getId(), sessionId, request.getMessage(), reply);
            return;
        }

        Map<String, Object> metadata = Map.of(
                "sessionId", sessionId,
                "mode", "live-deepseek",
                "provider", AiProviderType.DEEPSEEK.wireValue(),
                "promptMode", context.promptMode().wireValue()
        );
        try {
            writer.write("event: metadata\n");
            writer.write("data: " + objectMapper.writeValueAsString(metadata) + "\n\n");
            writer.flush();
        } catch (IOException ignored) {
        }

        StringBuilder replyBuilder = new StringBuilder();
        String msgId = "ds-msg-" + UUID.randomUUID();
        String userMessage = request.getMessage();
        String apiUserPrompt = resolveDeepSeekUserPrompt(user, userMessage, context);
        boolean[] streamedDelta = new boolean[]{false};
        Exception failure = null;
        try {
            deepSeekApiClient.chatCompletionStream(cozePromptLoader.botPersona(), apiUserPrompt, chunk -> {
                replyBuilder.append(chunk);
                streamedDelta[0] = true;
                try {
                    deepSeekApiClient.writeStreamDelta(writer, objectMapper, msgId, chunk);
                } catch (IOException ignored) {
                }
            });
        } catch (Exception ex) {
            failure = ex;
        }

        if (replyBuilder.isEmpty()) {
            try {
                String fallback = deepSeekApiClient.chatCompletion(cozePromptLoader.botPersona(), apiUserPrompt);
                if (StringUtils.hasText(fallback)) {
                    replyBuilder.append(fallback.trim());
                }
            } catch (Exception ex) {
                if (failure == null) {
                    failure = ex;
                }
            }
        }

        if (replyBuilder.isEmpty()) {
            String mockReply = cozeChatPromptService.buildMockReply(
                    request.getMessage(), context.referencedOrders(), user);
            if (StringUtils.hasText(mockReply)) {
                replyBuilder.append(mockReply.trim());
            }
        }

        if (replyBuilder.isEmpty()) {
            sendStreamError(writer, failure != null
                    ? failure
                    : new IOException("DeepSeek 未返回有效内容，请稍后重试"));
            return;
        }

        try {
            if (!streamedDelta[0]) {
                deepSeekApiClient.writeStreamDelta(writer, objectMapper, msgId, replyBuilder.toString());
            }
            writer.write("event: conversation.message.completed\n");
            writer.write("data: " + objectMapper.writeValueAsString(Map.of(
                    "id", msgId,
                    "role", "assistant",
                    "type", "answer",
                    "content", replyBuilder.toString(),
                    "content_type", "text"
            )) + "\n\n");
            writer.write("event: conversation.chat.completed\n");
            writer.write("data: {}\n\n");
            writer.flush();
            saveChatLog(user.getId(), sessionId, userMessage, replyBuilder.toString());
        } catch (Exception ex) {
            sendStreamError(writer, ex);
        }
    }

    private String resolveDeepSeekUserPrompt(SysUser user, String message, ChatContext context) {
        if (context.promptMode() == CozeChatPromptMode.REALTIME) {
            return cozeChatPromptService.buildCompactRealtimePrompt(user, message, context.referencedOrders());
        }
        return context.prompt();
    }

    public Map<String, Object> scheduling(CozeSchedulingRequest request) {
        List<ProdWorkOrder> workOrders = request.getWorkOrderIds().stream()
                .map(prodWorkOrderMapper::selectById)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<MatMaterial> warningMaterials = matMaterialMapper.selectList(new LambdaQueryWrapper<MatMaterial>()
                .eq(MatMaterial::getAlertStatus, "warning"));
        Map<String, Boolean> constraints = resolveSchedulingConstraints(request);
        LocalDate planDate = request.getPlanDate() != null ? request.getPlanDate() : LocalDate.now();

        try {
            if (deepSeekApiClient.isConfigured()) {
                Map<String, Object> parsed = runDeepSeekScheduling(workOrders, warningMaterials, planDate, constraints);
                Map<String, Object> finalized = cozeSchedulingService.finalizeSchedulingResult(
                        parsed, workOrders, constraints, planDate);
                return Map.of(
                        "mode", "live",
                        "provider", AiProviderType.DEEPSEEK.wireValue(),
                        "result", finalized,
                        "constraints", constraints
                );
            }
        } catch (Exception ex) {
            return mockSchedulingResult(workOrders, warningMaterials, constraints, planDate,
                    "DeepSeek 排产调用失败：" + ex.getMessage());
        }
        return mockSchedulingResult(workOrders, warningMaterials, constraints, planDate,
                "DeepSeek 未启用或未配置，当前展示演示排产结果");
    }

    public Map<String, Object> testChatHealth() {
        Map<String, Object> chat = new LinkedHashMap<>();
        chat.put("provider", AiProviderType.DEEPSEEK.wireValue());
        chat.put("model", cozeConfigService.getEffectiveDeepSeekModel());
        if (!deepSeekApiClient.isConfigured()) {
            chat.put("status", "skipped");
            chat.put("message", "未配置 DeepSeek API Key，跳过对话测试");
            return chat;
        }
        try {
            String reply = deepSeekApiClient.chatCompletion(
                    "你是连通性测试助手。",
                    "请只回复 OK");
            boolean ok = StringUtils.hasText(reply);
            chat.put("status", ok ? "ok" : "error");
            chat.put("message", ok ? "DeepSeek 对话测试成功，模型返回：" + truncate(reply, 80) : "DeepSeek 返回空内容");
        } catch (Exception ex) {
            chat.put("status", "error");
            chat.put("message", "DeepSeek 对话测试失败：" + ex.getMessage());
        }
        return chat;
    }

    public Map<String, Object> testSchedulingHealth() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("provider", AiProviderType.DEEPSEEK.wireValue());
        workflow.put("model", cozeConfigService.getEffectiveDeepSeekModel());
        if (!deepSeekApiClient.isConfigured()) {
            workflow.put("status", "skipped");
            workflow.put("message", "未配置 DeepSeek API Key，跳过排产测试");
            return workflow;
        }
        try {
            String reply = deepSeekApiClient.chatCompletion(
                    "你是排产 JSON 生成器。只输出 JSON，不要解释。",
                    "请输出一个包含 priorities、bottlenecks、dispatchSuggestions 三个空数组的 JSON 对象。");
            String json = deepSeekApiClient.stripMarkdownJson(reply);
            boolean hasKeys = json.contains("priorities") && json.contains("bottlenecks");
            workflow.put("status", hasKeys ? "ok" : "error");
            workflow.put("message", hasKeys
                    ? "DeepSeek 排产能力验证通过，模型可正确输出排产 JSON 结构"
                    : "DeepSeek 返回内容不含排产所需字段：" + truncate(reply, 120));
        } catch (Exception ex) {
            workflow.put("status", "error");
            workflow.put("message", "DeepSeek 排产测试失败：" + ex.getMessage());
        }
        return workflow;
    }

    private Map<String, Object> runDeepSeekScheduling(
            List<ProdWorkOrder> workOrders,
            List<MatMaterial> warningMaterials,
            LocalDate planDate,
            Map<String, Boolean> constraints) throws IOException, InterruptedException {
        Map<String, Object> parameters = cozeSchedulingService.buildWorkflowParameters(
                workOrders, warningMaterials, planDate, constraints);
        String systemPrompt = cozePromptLoader.renderSchedulingSystemPrompt(parameters);
        String userPrompt = cozePromptLoader.renderSchedulingUserPrompt(parameters);
        String raw = deepSeekApiClient.chatCompletion(systemPrompt, userPrompt);
        return cozeSchedulingService.parseSchedulingResponseText(raw);
    }

    private ChatContext buildChatContext(SysUser user, String message, String sessionId) {
        List<AiChatLog> sessionHistory = loadSessionHistory(user.getId(), sessionId);
        List<ProdWorkOrder> referencedOrders = cozeChatPromptService.resolveReferencedOrdersFromText(message);
        if (referencedOrders.isEmpty() && !cozeChatPromptService.isStandaloneKnowledgeQuestion(message)) {
            referencedOrders = cozeChatPromptService.resolveReferencedOrdersFromHistory(sessionHistory);
        }
        CozeChatPromptMode promptMode = cozeChatPromptService.resolvePromptMode(message, referencedOrders, sessionHistory);
        String prompt = cozeChatPromptService.buildChatPrompt(user, message, referencedOrders, promptMode, sessionHistory);
        return new ChatContext(prompt, promptMode, referencedOrders);
    }

    private Map<String, Boolean> resolveSchedulingConstraints(CozeSchedulingRequest request) {
        Map<String, Boolean> constraints = new LinkedHashMap<>();
        constraints.put("materialAvailability", request.getMaterialConstraint() == null || request.getMaterialConstraint());
        constraints.put("deviceLoad", request.getDeviceConstraint() == null || request.getDeviceConstraint());
        constraints.put("teamHours", request.getTeamConstraint() == null || request.getTeamConstraint());
        return constraints;
    }

    private Map<String, Object> mockSchedulingResult(
            List<ProdWorkOrder> workOrders,
            List<MatMaterial> warningMaterials,
            Map<String, Boolean> constraints,
            LocalDate planDate,
            String message) {
        Map<String, Object> mock = cozeSchedulingService.buildMockSchedulingPublic(
                workOrders, warningMaterials, constraints, planDate);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "mock");
        result.put("provider", AiProviderType.DEEPSEEK.wireValue());
        result.put("message", message);
        result.put("constraints", constraints);
        result.put("result", cozeSchedulingService.finalizeSchedulingResult(mock, workOrders, constraints, planDate));
        return result;
    }

    private void saveChatLog(Long userId, String sessionId, String userMessage, String aiResponse) {
        AiChatLog log = new AiChatLog();
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setUserMessage(userMessage);
        log.setAiResponse(aiResponse);
        log.setBotId(cozeConfigService.getEffectiveDeepSeekModel());
        log.setCreateTime(LocalDateTime.now());
        aiChatLogMapper.insert(log);
    }

    private List<AiChatLog> loadSessionHistory(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return List.of();
        }
        List<AiChatLog> recent = aiChatLogMapper.selectList(new LambdaQueryWrapper<AiChatLog>()
                .eq(AiChatLog::getUserId, userId)
                .eq(AiChatLog::getSessionId, sessionId)
                .orderByDesc(AiChatLog::getCreateTime)
                .last("limit " + CozeConstants.SESSION_HISTORY_TURNS));
        if (recent.isEmpty()) {
            return List.of();
        }
        List<AiChatLog> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);
        return chronological;
    }

    private void streamMockReply(PrintWriter writer, String reply, String sessionId, String promptMode) {
        Map<String, Object> metadata = Map.of(
                "sessionId", sessionId,
                "mode", "mock",
                "provider", AiProviderType.DEEPSEEK.wireValue(),
                "promptMode", promptMode
        );
        try {
            writer.write("event: metadata\n");
            writer.write("data: " + objectMapper.writeValueAsString(metadata) + "\n\n");
            writer.flush();
        } catch (IOException ignored) {
        }
        if (!StringUtils.hasText(reply)) {
            return;
        }
        String msgId = "mock-msg-" + UUID.randomUUID();
        int chunkSize = 2;
        for (int i = 0; i < reply.length(); i += chunkSize) {
            String chunk = reply.substring(i, Math.min(i + chunkSize, reply.length()));
            try {
                deepSeekApiClient.writeStreamDelta(writer, objectMapper, msgId, chunk);
                Thread.sleep(40);
            } catch (Exception e) {
                break;
            }
        }
        try {
            writer.write("event: conversation.message.completed\n");
            writer.write("data: " + objectMapper.writeValueAsString(Map.of(
                    "id", msgId,
                    "role", "assistant",
                    "type", "answer",
                    "content", reply,
                    "content_type", "text"
            )) + "\n\n");
            writer.write("event: conversation.chat.completed\n");
            writer.write("data: {}\n\n");
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private void sendStreamError(PrintWriter writer, Exception ex) {
        String detail = ex.getMessage() == null ? "" : ex.getMessage();
        String errorMsg = detail.contains("timeout") ? "AI 响应超时，请稍后重试" : "DeepSeek 对话失败：" + detail;
        try {
            writer.write("event: error\n");
            writer.write("data: " + objectMapper.writeValueAsString(Map.of("message", errorMsg)) + "\n\n");
            writer.flush();
        } catch (Exception ignored) {
        }
    }

    private String truncate(String text, int max) {
        if (!StringUtils.hasText(text) || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

    private record ChatContext(String prompt, CozeChatPromptMode promptMode, List<ProdWorkOrder> referencedOrders) {
    }
}
