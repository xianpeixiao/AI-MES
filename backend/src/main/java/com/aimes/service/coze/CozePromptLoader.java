package com.aimes.service.coze;

import com.aimes.config.AimesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 加载与 Coze 平台一致的 Bot 人设、排产工作流提示词。
 * 优先读取项目 docs/ 下的 txt（与 Coze 控制台配置同源），否则回退 classpath。
 */
@Component
@RequiredArgsConstructor
public class CozePromptLoader {

    private static final String DEFAULT_BOT_PERSONA = "prompts/coze-bot-persona.txt";
    private static final String DEFAULT_SCHEDULING_SYSTEM = "prompts/coze-scheduling-system-prompt.txt";
    private static final String DEFAULT_SCHEDULING_USER = "prompts/coze-scheduling-user-prompt.txt";

    private final AimesProperties aimesProperties;

    private volatile String botPersonaCache;
    private volatile String schedulingSystemCache;
    private volatile String schedulingUserCache;

    public String botPersona() {
        if (botPersonaCache != null) {
            return botPersonaCache;
        }
        synchronized (this) {
            if (botPersonaCache != null) {
                return botPersonaCache;
            }
            botPersonaCache = sanitizeBotPersona(readBotPersonaRaw());
            return botPersonaCache;
        }
    }

    public String schedulingSystemTemplate() {
        if (schedulingSystemCache != null) {
            return schedulingSystemCache;
        }
        synchronized (this) {
            if (schedulingSystemCache != null) {
                return schedulingSystemCache;
            }
            schedulingSystemCache = readSchedulingSystemRaw();
            return schedulingSystemCache;
        }
    }

    public String schedulingUserTemplate() {
        if (schedulingUserCache != null) {
            return schedulingUserCache;
        }
        synchronized (this) {
            if (schedulingUserCache != null) {
                return schedulingUserCache;
            }
            schedulingUserCache = readSchedulingUserRaw();
            return schedulingUserCache;
        }
    }

    public String renderSchedulingSystemPrompt(Map<String, Object> parameters) {
        return applyTemplate(schedulingSystemTemplate(), parameters);
    }

    public String renderSchedulingUserPrompt(Map<String, Object> parameters) {
        return applyTemplate(schedulingUserTemplate(), parameters);
    }

    public String applyTemplate(String template, Map<String, Object> parameters) {
        if (!StringUtils.hasText(template) || parameters == null || parameters.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("{{" + key + "}}", value);
        }
        return rendered;
    }

    private String readBotPersonaRaw() {
        Path configured = configuredPath(aimesProperties.getCozeBotPersonaPath());
        if (configured != null) {
            return readFile(configured);
        }
        Path docs = firstExistingProjectDoc("coze智能体的人设提示词.txt");
        if (docs != null) {
            return readFile(docs);
        }
        return readClasspath(DEFAULT_BOT_PERSONA);
    }

    private String readSchedulingSystemRaw() {
        Path configured = configuredPath(aimesProperties.getSchedulingSystemPromptPath());
        if (configured != null) {
            return readFile(configured);
        }
        Path docs = firstExistingProjectDoc("排产工作流系统提示词.txt");
        if (docs != null) {
            return readFile(docs);
        }
        return readClasspath(DEFAULT_SCHEDULING_SYSTEM);
    }

    private String readSchedulingUserRaw() {
        Path configured = configuredPath(aimesProperties.getSchedulingUserPromptPath());
        if (configured != null) {
            return readFile(configured);
        }
        Path docs = firstExistingProjectDoc(
                "排产工作流用户提示词.txt.txt",
                "排产工作流用户提示词.txt");
        if (docs != null) {
            return readFile(docs);
        }
        return readClasspath(DEFAULT_SCHEDULING_USER);
    }

    private String sanitizeBotPersona(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        return raw.replaceAll("\\{#LibraryBlock[^#]*#}", "")
                .replaceAll("\\{#/LibraryBlock#}", "")
                .trim();
    }

    private Path configuredPath(String configured) {
        if (!StringUtils.hasText(configured)) {
            return null;
        }
        Path path = Paths.get(configured.trim());
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        return Files.isRegularFile(path) ? path : null;
    }

    private Path firstExistingProjectDoc(String... fileNames) {
        for (String fileName : fileNames) {
            for (Path candidate : projectDocCandidates(fileName)) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<Path> projectDocCandidates(String fileName) {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve("docs").resolve(fileName));
        if (cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve("docs").resolve(fileName));
        }
        if ("backend".equalsIgnoreCase(cwd.getFileName().toString()) && cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve("docs").resolve(fileName));
        }
        return candidates;
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取提示词文件失败：" + path, ex);
        }
    }

    private String readClasspath(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("读取 classpath 提示词失败：" + resourcePath, ex);
        }
    }
}
