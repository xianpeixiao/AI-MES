package com.aimes.service.coze;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/** Coze 子包内共享常量，避免各 Service 重复定义。 */
public final class CozeConstants {

    public static final Pattern ORDER_NO_PATTERN = Pattern.compile("WO-\\d{4}-\\d{3}");

    public static final int SESSION_HISTORY_TURNS = 5;
    public static final int SESSION_HISTORY_MAX_CHARS = 500;

    public static final DateTimeFormatter SCHEDULING_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final int DEFAULT_DISPATCH_HOURS = 2;

    /** 与 docs/coze智能体的人设提示词.txt §4 一致，供 buildChatPrompt 注入。 */
    public static final String MARKDOWN_ANSWER_FORMAT_RULE =
            "- 按 Bot 人设 §4 排版：Markdown + emoji 分节（如 📌 说明、🔍 原因、🛠️ 步骤、💡 补充），"
                    + "禁止大段纯文字与【问题分析】【处理建议】【注意事项】等报告体标题。\n";

    public static final String REALTIME_MARKDOWN_ANSWER_FORMAT_RULE =
            "- 使用 Markdown + emoji 排版（如 📊 数据概览、📋 明细列表、💡 补充说明），"
                    + "禁止【问题分析】【处理建议】【注意事项】报告体。\n";

    private CozeConstants() {
    }
}
