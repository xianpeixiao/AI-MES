package com.aimes.service.knowledge;

import com.aimes.config.AimesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 从项目 docs/knowledge-base 加载 Q&A 手册，并按关键词检索相关片段。
 * Coze / DeepSeek 等 AI 引擎共用同一份本地知识源。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private static final String BLOCK_DELIMITER = "###";

    private final AimesProperties aimesProperties;

    private volatile List<KnowledgeEntry> cachedEntries = List.of();
    private volatile Path cachedDir;

    public boolean isLoaded() {
        return !loadEntries().isEmpty();
    }

    public Path resolvedDirectory() {
        return resolveKnowledgeDir();
    }

    public int entryCount() {
        return loadEntries().size();
    }

    public List<KnowledgeEntry> search(String query) {
        return search(query, aimesProperties.getKnowledgeTopK());
    }

    public List<KnowledgeEntry> search(String query, int limit) {
        List<KnowledgeEntry> entries = loadEntries();
        if (entries.isEmpty() || !StringUtils.hasText(query) || limit <= 0) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        return entries.stream()
                .map(entry -> new ScoredEntry(entry, score(normalizedQuery, entry)))
                .filter(item -> item.score > 0)
                .sorted(Comparator.comparingInt(ScoredEntry::score).reversed())
                .limit(limit)
                .map(item -> item.entry)
                .toList();
    }

    private List<KnowledgeEntry> loadEntries() {
        Path dir = resolveKnowledgeDir();
        if (dir == null) {
            cachedEntries = List.of();
            cachedDir = null;
            return cachedEntries;
        }
        if (dir.equals(cachedDir) && !cachedEntries.isEmpty()) {
            return cachedEntries;
        }
        synchronized (this) {
            if (dir.equals(cachedDir) && !cachedEntries.isEmpty()) {
                return cachedEntries;
            }
            cachedEntries = readDirectory(dir);
            cachedDir = dir;
            return cachedEntries;
        }
    }

    private List<KnowledgeEntry> readDirectory(Path dir) {
        List<KnowledgeEntry> entries = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> entries.addAll(parseFile(path)));
        } catch (IOException ex) {
            cachedEntries = List.of();
            cachedDir = dir;
            return cachedEntries;
        }
        return List.copyOf(entries);
    }

    private List<KnowledgeEntry> parseFile(Path file) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return List.of();
        }
        String fileName = file.getFileName().toString();
        List<KnowledgeEntry> entries = new ArrayList<>();
        for (String block : content.split(BLOCK_DELIMITER)) {
            KnowledgeEntry entry = parseBlock(fileName, block);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private KnowledgeEntry parseBlock(String fileName, String block) {
        String question = null;
        StringBuilder answer = new StringBuilder();
        for (String rawLine : block.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("问：") || line.startsWith("问:")) {
                question = line.substring(2).trim();
                continue;
            }
            if (line.startsWith("答：") || line.startsWith("答:")) {
                if (answer.length() > 0) {
                    answer.append('\n');
                }
                answer.append(line.substring(2).trim());
                continue;
            }
            if (question != null && answer.length() > 0) {
                answer.append('\n').append(line);
            }
        }
        if (!StringUtils.hasText(question) || answer.length() == 0) {
            return null;
        }
        return new KnowledgeEntry(fileName, question.trim(), answer.toString().trim());
    }

    private Path resolveKnowledgeDir() {
        String configured = aimesProperties.getKnowledgeBaseDir();
        if (StringUtils.hasText(configured)) {
            Path configuredPath = Paths.get(configured.trim());
            if (!configuredPath.isAbsolute()) {
                configuredPath = Paths.get(System.getProperty("user.dir")).resolve(configuredPath).normalize();
            }
            return Files.isDirectory(configuredPath) ? configuredPath : null;
        }
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve("docs/knowledge-base"));
        if (cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve("docs/knowledge-base"));
        }
        candidates.add(cwd.resolve("../docs/knowledge-base").normalize());
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.normalize();
            }
        }
        return null;
    }

    private int score(String query, KnowledgeEntry entry) {
        String corpus = normalize(entry.question() + " " + entry.answer());
        int score = 0;
        if (corpus.contains(query)) {
            score += 20;
        }
        for (int i = 0; i < query.length() - 1; i++) {
            String token = query.substring(i, i + 2).trim();
            if (token.length() < 2) {
                continue;
            }
            if (corpus.contains(token)) {
                score += 2;
            }
        }
        for (String keyword : extractKeywords(query)) {
            if (corpus.contains(keyword)) {
                score += 5;
            }
        }
        return score;
    }

    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        String[] parts = query.split("[\\s,，。！？；;、/\\\\|]+");
        for (String part : parts) {
            String token = part.trim();
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredEntry(KnowledgeEntry entry, int score) {
    }
}
