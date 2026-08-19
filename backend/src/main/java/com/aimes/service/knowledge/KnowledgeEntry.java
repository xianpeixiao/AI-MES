package com.aimes.service.knowledge;

/**
 * 从 docs/knowledge-base 解析出的一条问答。
 */
public record KnowledgeEntry(String sourceFile, String question, String answer) {
}
