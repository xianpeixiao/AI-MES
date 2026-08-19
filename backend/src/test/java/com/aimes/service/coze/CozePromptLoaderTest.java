package com.aimes.service.coze;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CozePromptLoaderTest {

    private final CozePromptLoader loader = new CozePromptLoader(new com.aimes.config.AimesProperties());

    @Test
    void botPersona_stripsCozeLibraryBlocks() {
        String persona = loader.botPersona();
        assertFalse(persona.contains("{#LibraryBlock"));
        assertTrue(persona.contains("AI-MES 智能生产助手"));
        assertTrue(persona.contains("回答格式"));
    }

    @Test
    void schedulingTemplates_replaceWorkflowVariables() {
        Map<String, Object> parameters = Map.of(
                "plan_date", "2026-08-19",
                "current_time", "2026-08-19 10:00",
                "work_orders_json", "[]",
                "material_status", "[]",
                "constraints_json", "{}",
                "teams_json", "[]",
                "devices_json", "[]");

        String systemPrompt = loader.renderSchedulingSystemPrompt(parameters);
        String userPrompt = loader.renderSchedulingUserPrompt(parameters);

        assertFalse(systemPrompt.contains("{{plan_date}}"));
        assertTrue(systemPrompt.contains("2026-08-19"));
        assertTrue(systemPrompt.contains("priorities"));
        assertTrue(systemPrompt.contains("dispatchSuggestions"));

        assertFalse(userPrompt.contains("{{work_orders_json}}"));
        assertTrue(userPrompt.contains("[]"));
        assertTrue(userPrompt.contains("materialAvailability=true"));
    }
}
