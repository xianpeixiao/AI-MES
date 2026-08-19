package com.aimes.service.knowledge;

import com.aimes.config.AimesProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeRetrievalServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void search_returnsMatchingEntries() throws IOException {
        Path knowledgeDir = tempDir.resolve("knowledge-base");
        Files.createDirectories(knowledgeDir);
        Files.writeString(knowledgeDir.resolve("demo.md"), """
                ###
                问：演示账号密码是什么？
                答：五个演示账号密码均为 123456。

                ###
                问：怎么下发生产计划？
                答：进入生产计划，找到草稿计划，点击下发确认。
                """, StandardCharsets.UTF_8);

        KnowledgeRetrievalService service = createService(knowledgeDir.toString());

        assertTrue(service.isLoaded());
        assertEquals(2, service.entryCount());

        List<KnowledgeEntry> hits = service.search("演示账号密码", 2);
        assertFalse(hits.isEmpty());
        assertTrue(hits.get(0).answer().contains("123456"));
    }

    @Test
    void search_returnsEmptyWhenDirectoryMissing() {
        KnowledgeRetrievalService service = createService(tempDir.resolve("missing").toString());
        assertFalse(service.isLoaded());
        assertTrue(service.search("任意问题").isEmpty());
    }

    private KnowledgeRetrievalService createService(String dir) {
        AimesProperties properties = new AimesProperties();
        properties.setKnowledgeBaseDir(dir);
        properties.setKnowledgeTopK(5);
        return new KnowledgeRetrievalService(properties);
    }
}
