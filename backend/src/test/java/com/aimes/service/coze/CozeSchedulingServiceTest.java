package com.aimes.service.coze;

import com.aimes.entity.ProdTeam;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.mapper.ExcEventMapper;
import com.aimes.mapper.MatMaterialMapper;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.service.CozeConfigService;
import com.aimes.service.DeviceService;
import com.aimes.service.ai.DeepSeekEngineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CozeSchedulingServiceTest {

    @Mock
    private ProdWorkOrderMapper prodWorkOrderMapper;
    @Mock
    private ProdTeamMapper prodTeamMapper;
    @Mock
    private MatMaterialMapper matMaterialMapper;
    @Mock
    private ExcEventMapper excEventMapper;
    @Mock
    private CozeConfigService cozeConfigService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private CozeApiClient cozeApiClient;
    @Mock
    private ObjectProvider<DeepSeekEngineService> deepSeekEngineService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CozeSchedulingService cozeSchedulingService;

    @BeforeEach
    void stubTeamLoads() {
        ProdTeam team = new ProdTeam();
        team.setId(1L);
        team.setTeamName("甲班");
        team.setMemberCount(6);
        when(prodTeamMapper.selectList(any())).thenReturn(List.of(team));
        when(prodWorkOrderMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void finalizeSchedulingResult_whenMaterialDisabled_shouldStripMaterialReferences() {
        Map<String, Object> parsed = sampleSchedulingPayload();
        Map<String, Boolean> constraints = constraints(false, true, true);

        Map<String, Object> result = cozeSchedulingService.finalizeSchedulingResult(
                parsed,
                List.of(sampleWorkOrder("WO-001")),
                constraints,
                LocalDate.of(2026, 8, 19));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> priorities = (List<Map<String, Object>>) result.get("priorities");
        String reason = String.valueOf(priorities.get(0).get("reason"));
        assertFalse(reason.contains("缺料"));
        assertTrue(reason.contains("未纳入物料可用性约束"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bottlenecks = (List<Map<String, Object>>) result.get("bottlenecks");
        assertTrue(bottlenecks.stream().anyMatch(row ->
                String.valueOf(row.get("processName")).contains("物料约束")));
        assertTrue(String.valueOf(result.get("summary")).contains("未纳入物料可用性约束"));
    }

    @Test
    void finalizeSchedulingResult_whenTeamHoursDisabled_shouldMarkHoursPending() {
        Map<String, Object> parsed = sampleSchedulingPayload();
        Map<String, Boolean> constraints = constraints(true, true, false);

        Map<String, Object> result = cozeSchedulingService.finalizeSchedulingResult(
                parsed,
                List.of(sampleWorkOrder("WO-001")),
                constraints,
                LocalDate.of(2026, 8, 19));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dispatches = (List<Map<String, Object>>) result.get("dispatchSuggestions");
        assertEquals("待定", dispatches.get(0).get("hours"));
        assertTrue(String.valueOf(result.get("summary")).contains("未纳入班组工时约束"));
    }

    @Test
    void buildMockSchedulingPublic_shouldReturnStructuredResult() {
        ProdWorkOrder order = sampleWorkOrder("WO-010");
        Map<String, Boolean> constraints = constraints(true, true, true);

        Map<String, Object> mock = cozeSchedulingService.buildMockSchedulingPublic(
                List.of(order),
                List.of(),
                constraints,
                LocalDate.of(2026, 8, 19));

        assertFalse(((List<?>) mock.get("priorities")).isEmpty());
        assertFalse(((List<?>) mock.get("dispatches")).isEmpty());
        assertFalse(((List<?>) mock.get("bottlenecks")).isEmpty());
    }

    @Test
    void parseSchedulingResponseText_shouldParsePlainJson() throws Exception {
        when(cozeApiClient.stripMarkdownJson(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> parsed = cozeSchedulingService.parseSchedulingResponseText("""
                {
                  "priorities": [{"workOrderCode":"WO-001","rank":1}],
                  "bottlenecks": [],
                  "dispatchSuggestions": []
                }
                """);

        assertEquals(1, ((List<?>) parsed.get("priorities")).size());
        assertTrue(parsed.containsKey("dispatchSuggestions"));
    }

    @Test
    void summarizeSchedulingResult_shouldCountSections() {
        Map<String, Object> parsed = sampleSchedulingPayload();

        Map<String, Object> summary = cozeSchedulingService.summarizeSchedulingResult(parsed);

        assertEquals(1, summary.get("priorities"));
        assertEquals(1, summary.get("bottlenecks"));
        assertEquals(1, summary.get("dispatches"));
    }

    private Map<String, Object> sampleSchedulingPayload() {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("priorities", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of(
                "workOrderCode", "WO-001",
                "rank", 1,
                "priorityLabel", "高",
                "reason", "物料缺料需优先处理"
        )))));
        parsed.put("bottlenecks", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of(
                "processName", "装配工序",
                "loadRate", 88,
                "suggestion", "请优先处理缺料后再集中排装配任务"
        )))));
        parsed.put("dispatchSuggestions", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of(
                "workOrderCode", "WO-001",
                "teamName", "待定",
                "startTime", "2026-08-19 08:00",
                "hours", "4"
        )))));
        return parsed;
    }

    private Map<String, Boolean> constraints(boolean material, boolean device, boolean team) {
        Map<String, Boolean> constraints = new LinkedHashMap<>();
        constraints.put("materialAvailability", material);
        constraints.put("deviceLoad", device);
        constraints.put("teamHours", team);
        return constraints;
    }

    private ProdWorkOrder sampleWorkOrder(String orderNo) {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setPriority(2);
        order.setStatus("pending");
        return order;
    }
}
