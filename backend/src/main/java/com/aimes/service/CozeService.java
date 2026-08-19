package com.aimes.service;

import com.aimes.dto.request.coze.CozeChatRequest;
import com.aimes.dto.request.coze.CozeSchedulingRequest;
import com.aimes.service.ai.DeepSeekEngineService;
import com.aimes.service.coze.CozeChatService;
import com.aimes.service.coze.CozeHealthService;
import com.aimes.service.coze.CozeSchedulingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** AI 门面：Controller 仅依赖此类，Coze / DeepSeek 实现已拆分至子包。 */
@Service
@RequiredArgsConstructor
public class CozeService {

    private final CozeChatService cozeChatService;
    private final CozeSchedulingService cozeSchedulingService;
    private final CozeHealthService cozeHealthService;
    private final DeepSeekEngineService deepSeekEngineService;

    public Map<String, Object> chat(CozeChatRequest request) {
        return cozeChatService.chat(request);
    }

    public void chatStream(CozeChatRequest request, HttpServletResponse response) {
        cozeChatService.chatStream(request, response);
    }

    public List<Map<String, Object>> history(String sessionId) {
        return cozeChatService.history(sessionId);
    }

    public int deleteSession(String sessionId) {
        return cozeChatService.deleteSession(sessionId);
    }

    public Map<String, Object> scheduling(CozeSchedulingRequest request) {
        return cozeSchedulingService.scheduling(request);
    }

    public Map<String, Object> schedulingContext(List<Long> workOrderIds) {
        return cozeSchedulingService.schedulingContext(workOrderIds);
    }

    public Map<String, Object> health() {
        return cozeHealthService.health();
    }

    public Map<String, Object> healthChat() {
        return cozeChatService.testChatHealth();
    }

    public Map<String, Object> healthWorkflow() {
        return cozeSchedulingService.testWorkflowHealth();
    }

    public Map<String, Object> healthCoze() {
        return cozeHealthService.cozeHealth();
    }

    public Map<String, Object> healthDeepSeek() {
        return cozeHealthService.deepSeekHealth();
    }

    public Map<String, Object> healthDeepSeekChat() {
        return deepSeekEngineService.testChatHealth();
    }

    public Map<String, Object> healthDeepSeekScheduling() {
        return deepSeekEngineService.testSchedulingHealth();
    }
}
