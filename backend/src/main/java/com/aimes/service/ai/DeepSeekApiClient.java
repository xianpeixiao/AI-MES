package com.aimes.service.ai;

import com.aimes.service.CozeConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DeepSeekApiClient {

    private final CozeConfigService cozeConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public DeepSeekApiClient(CozeConfigService cozeConfigService, ObjectMapper objectMapper) {
        this.cozeConfigService = cozeConfigService;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return cozeConfigService.isDeepSeekConfigured();
    }

    public String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        List<Map<String, String>> messages = buildMessages(systemPrompt, userPrompt);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model());
        payload.put("messages", messages);
        payload.put("temperature", 0.3);
        payload.put("stream", false);

        JsonNode root = invokeJson(completionsUrl(), payload);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
            throw new IOException("DeepSeek 未返回有效内容");
        }
        return content.asText().trim();
    }

    public void chatCompletionStream(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onDelta) throws IOException, InterruptedException {
        List<Map<String, String>> messages = buildMessages(systemPrompt, userPrompt);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model());
        payload.put("messages", messages);
        payload.put("temperature", 0.3);
        payload.put("stream", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(completionsUrl()))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (InputStream is = response.body()) {
                String errBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("DeepSeek API 请求失败: HTTP " + response.statusCode() + " - " + errBody);
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode node = objectMapper.readTree(data);
                if (node.has("error")) {
                    String err = node.path("error").path("message").asText("DeepSeek 返回错误");
                    throw new IOException(err);
                }
                String chunk = extractStreamContent(node);
                if (StringUtils.hasText(chunk)) {
                    onDelta.accept(chunk);
                }
            }
        }
    }

    public String stripMarkdownJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start >= 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }
        return text;
    }

    public void writeStreamDelta(PrintWriter writer, ObjectMapper mapper, String msgId, String chunk) throws IOException {
        Map<String, Object> delta = Map.of(
                "id", msgId,
                "role", "assistant",
                "type", "answer",
                "content", chunk,
                "content_type", "text"
        );
        writer.write("event: conversation.message.delta\n");
        writer.write("data: " + mapper.writeValueAsString(delta) + "\n\n");
        writer.flush();
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        return messages;
    }

    private JsonNode invokeJson(String url, Map<String, Object> payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("DeepSeek API 请求失败: HTTP " + response.statusCode() + " - " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private String extractStreamContent(JsonNode node) {
        JsonNode choice = node.path("choices").path(0);
        JsonNode delta = choice.path("delta");
        JsonNode deltaContent = delta.path("content");
        if (deltaContent.isTextual() && StringUtils.hasText(deltaContent.asText())) {
            return deltaContent.asText();
        }
        JsonNode messageContent = choice.path("message").path("content");
        if (messageContent.isTextual() && StringUtils.hasText(messageContent.asText())) {
            return messageContent.asText();
        }
        JsonNode textNode = delta.path("text");
        if (textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
            return textNode.asText();
        }
        return "";
    }

    private String completionsUrl() {
        String base = apiUrl().replaceAll("/+$", "");
        return base + "/chat/completions";
    }

    private String apiKey() {
        return cozeConfigService.getEffectiveDeepSeekApiKey();
    }

    private String apiUrl() {
        return cozeConfigService.getEffectiveDeepSeekApiUrl();
    }

    private String model() {
        return cozeConfigService.getEffectiveDeepSeekModel();
    }
}
