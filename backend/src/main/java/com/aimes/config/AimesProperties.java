package com.aimes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aimes")
public class AimesProperties {

    private String uploadDir = "uploads";
    private int planSplitQty = 100;
    /** 生产执行时按工序领料；无工序领料记录时工单完工回退按 BOM */
    private boolean bomPickOnComplete = true;
    /** 本地知识库目录，留空则自动探测项目根 docs/knowledge-base */
    private String knowledgeBaseDir = "";
    /** 知识库问答注入 Prompt 的条目数 */
    private int knowledgeTopK = 5;
    /** Coze Bot 人设 txt，留空则自动读取 docs/coze智能体的人设提示词.txt */
    private String cozeBotPersonaPath = "";
    /** 排产工作流系统提示词 txt，留空则自动读取 docs/排产工作流系统提示词.txt */
    private String schedulingSystemPromptPath = "";
    /** 排产工作流用户提示词 txt，留空则自动读取 docs/排产工作流用户提示词.txt */
    private String schedulingUserPromptPath = "";
}
