package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.common.OperationLogRunner;
import com.aimes.config.AimesProperties;
import com.aimes.dto.request.coze.SchedulingApplyRequest;
import com.aimes.dto.request.coze.SchedulingDispatchItem;
import com.aimes.dto.request.coze.SchedulingPriorityItem;
import com.aimes.dto.request.workorder.WorkOrderAssignRequest;
import com.aimes.dto.request.workorder.WorkOrderProgressRequest;
import com.aimes.entity.ProdProcessRecord;
import com.aimes.entity.ProdTeam;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ExcEventMapper;
import com.aimes.mapper.ProdPlanMapper;
import com.aimes.mapper.ProdProcessRecordMapper;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.Callable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderServiceTest {

    @Mock
    private ProdWorkOrderMapper prodWorkOrderMapper;
    @Mock
    private ProdPlanMapper prodPlanMapper;
    @Mock
    private ProdTeamMapper prodTeamMapper;
    @Mock
    private ProdProcessRecordMapper prodProcessRecordMapper;
    @Mock
    private ExcEventMapper excEventMapper;
    @Mock
    private AuthService authService;
    @Mock
    private WorkOrderNoService workOrderNoService;
    @Mock
    private SysNotificationService sysNotificationService;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ProcessRouteService processRouteService;
    @Mock
    private OperationLogRunner operationLogRunner;
    @Mock
    private ProductService productService;
    @Mock
    private MaterialService materialService;
    @Mock
    private AimesProperties aimesProperties;

    @InjectMocks
    private WorkOrderService workOrderService;

    @org.junit.jupiter.api.BeforeEach
    void setUpRunner() throws Exception {
        lenient().when(operationLogRunner.runUnchecked(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(4);
            return callable.call();
        });
    }

    @Test
    void assign_shouldMovePendingOrderToAssigned() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-001");
        order.setStatus("pending");
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        stubDetailQueries(order);

        WorkOrderAssignRequest request = new WorkOrderAssignRequest();
        request.setTeamId(2L);
        request.setPriority(2);

        Map<String, Object> result = workOrderService.assign(1L, request);

        assertEquals("assigned", order.getStatus());
        assertEquals(2L, order.getTeamId());
        assertEquals("assigned", result.get("status"));
        verify(prodWorkOrderMapper).updateById(order);
    }

    @Test
    void claim_shouldMoveAssignedOrderToProducingWithoutStartingProcess() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-002");
        order.setStatus("assigned");
        order.setTeamId(2L);

        ProdProcessRecord first = new ProdProcessRecord();
        first.setId(10L);
        first.setWorkOrderId(1L);
        first.setSeqNo(1);
        first.setProcessName("备料");
        first.setStatus("waiting");

        SysUser user = new SysUser();
        user.setId(5L);
        user.setRole("admin");

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of(first));
        stubDetailQueries(order);

        Map<String, Object> result = workOrderService.claim(1L);

        assertEquals("producing", order.getStatus());
        assertEquals(5L, order.getClaimUserId());
        assertEquals("waiting", first.getStatus());
        assertEquals("producing", result.get("status"));
        verify(prodWorkOrderMapper).updateById(order);
        verify(prodProcessRecordMapper, never()).updateById(any(ProdProcessRecord.class));
    }

    @Test
    void complete_whenAlreadyDone_shouldSkipPickAndUpdates() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-DONE");
        order.setStatus("done");
        order.setProgress(100);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        stubDetailQueries(order);

        Map<String, Object> result = workOrderService.complete(1L);

        assertEquals("done", result.get("status"));
        verify(materialService, never()).pickForWorkOrder(anyLong(), anyLong(), anyInt(), any());
        verify(productService, never()).receiveFromWorkOrder(anyLong(), anyLong(), anyInt(), any());
        verify(prodWorkOrderMapper, never()).updateById(order);
    }

    @Test
    void complete_whenProducing_shouldPickMaterialsOnce() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(2L);
        order.setOrderNo("WO-PROD");
        order.setStatus("producing");
        order.setProgress(80);
        order.setProductId(10L);
        order.setOrderQty(1);
        when(prodWorkOrderMapper.selectById(2L)).thenReturn(order);
        when(aimesProperties.isBomPickOnComplete()).thenReturn(true);
        when(materialService.hasPickActivity(eq(2L), anyCollection())).thenReturn(false);
        when(processRouteService.resolveRouting(10L, null)).thenReturn(ProcessRouteService.RoutingContext.fallback());
        when(productService.hasActiveBom(10L)).thenReturn(true);
        when(productService.computeBomDemand(10L, 1)).thenReturn(List.of(Map.of("materialId", 1L, "requiredQty", 1)));
        stubDetailQueries(order);

        workOrderService.complete(2L);

        assertEquals("done", order.getStatus());
        verify(materialService).pickForWorkOrder(2L, 10L, 1, List.of(Map.of("materialId", 1L, "requiredQty", 1)), "bom");
        verify(productService).receiveFromWorkOrder(2L, 10L, 1, "WO-PROD");
        verify(prodWorkOrderMapper).updateById(order);
    }

    @Test
    void updateProgress_shouldRejectWorkerUpdatingOtherTeamOrder() {
        SysUser worker = new SysUser();
        worker.setRole("worker");
        worker.setTeamId(1L);

        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setTeamId(2L);
        order.setStatus("producing");
        order.setProcessName("备料");

        when(authService.currentUser()).thenReturn(worker);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);

        WorkOrderProgressRequest request = new WorkOrderProgressRequest();
        request.setProcessName("备料");
        request.setProgress(10);

        assertThrows(BusinessException.class, () -> workOrderService.updateProgress(1L, request));
    }

    @Test
    void updateProgress_shouldRejectOutOfSequenceProcess() {
        SysUser user = adminUser();
        ProdWorkOrder order = producingOrder("备料");
        ProdProcessRecord current = processRecord(10L, 1, "备料", "waiting");
        ProdProcessRecord next = processRecord(11L, 2, "装配", "waiting");

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of(current, next));

        WorkOrderProgressRequest request = new WorkOrderProgressRequest();
        request.setProcessName("装配");
        request.setProgress(20);

        assertThrows(BusinessException.class, () -> workOrderService.updateProgress(1L, request));
    }

    @Test
    void updateProgress_whenStartingProcess_shouldMarkRunningAndPickMaterials() {
        SysUser user = adminUser();
        ProdWorkOrder order = producingOrder("备料");
        order.setOrderQty(2);
        ProdProcessRecord current = processRecord(10L, 1, "备料", "waiting");
        current.setOperationId(100L);

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of(current));
        when(prodProcessRecordMapper.selectOne(any())).thenReturn(current);
        when(aimesProperties.isBomPickOnComplete()).thenReturn(true);
        when(processRouteService.resolveRouting(any(), any())).thenReturn(ProcessRouteService.RoutingContext.fallback());
        when(processRouteService.hasDeviceBindings(100L)).thenReturn(false);
        when(processRouteService.computeOperationMaterialDemand(100L, 2))
                .thenReturn(List.of(Map.of("materialId", 1L, "requiredQty", 2)));
        stubDetailQueries(order);

        WorkOrderProgressRequest request = new WorkOrderProgressRequest();
        request.setProcessName("备料");
        request.setProgress(10);
        request.setCompleteCurrentProcess(false);

        workOrderService.updateProgress(1L, request);

        assertEquals("running", current.getStatus());
        assertTrue(current.getStartTime() != null);
        verify(materialService).pickForProcess(eq(1L), eq(10L), any(), any());
        verify(prodProcessRecordMapper).updateById(current);
    }

    @Test
    void updateProgress_whenCompletingProcess_shouldAdvanceToNextProcess() {
        SysUser user = adminUser();
        ProdWorkOrder order = producingOrder("备料");
        ProdProcessRecord current = processRecord(10L, 1, "备料", "running");
        current.setOperationId(100L);
        current.setStartTime(java.time.LocalDateTime.now());
        ProdProcessRecord next = processRecord(11L, 2, "装配", "waiting");

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of(current, next));
        when(prodProcessRecordMapper.selectOne(any())).thenReturn(current, next);
        when(processRouteService.resolveRouting(any(), any())).thenReturn(ProcessRouteService.RoutingContext.fallback());
        when(processRouteService.hasDeviceBindings(any())).thenReturn(false);
        when(processRouteService.getExecutionContext(1L)).thenReturn(Map.of());
        when(excEventMapper.selectList(any())).thenReturn(List.of());
        when(productService.getProductSummary(10L)).thenReturn(Map.of("productName", "测试产品"));
        when(productService.computeBomDemand(eq(10L), anyInt())).thenReturn(List.of());
        when(productService.hasActiveBom(10L)).thenReturn(false);
        lenient().when(prodPlanMapper.selectById(any())).thenReturn(null);
        lenient().when(prodTeamMapper.selectById(any())).thenReturn(null);
        lenient().when(excEventMapper.selectCount(any())).thenReturn(0L);

        WorkOrderProgressRequest request = new WorkOrderProgressRequest();
        request.setProcessName("备料");
        request.setProgress(50);
        request.setCompleteCurrentProcess(true);

        workOrderService.updateProgress(1L, request);

        assertEquals("done", current.getStatus());
        assertEquals("装配", order.getProcessName());
        assertEquals(50, order.getProgress());
    }

    @Test
    void updateProgress_shouldRejectProgressAboveAllowedMaximum() {
        SysUser user = adminUser();
        ProdWorkOrder order = producingOrder("备料");
        ProdProcessRecord current = processRecord(10L, 1, "备料", "waiting");
        current.setOperationId(100L);
        ProdProcessRecord next = processRecord(11L, 2, "装配", "waiting");

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(1L)).thenReturn(order);
        when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of(current, next));
        when(prodProcessRecordMapper.selectOne(any())).thenReturn(current);
        when(processRouteService.resolveRouting(any(), any())).thenReturn(ProcessRouteService.RoutingContext.fallback());
        when(processRouteService.hasDeviceBindings(any())).thenReturn(false);

        WorkOrderProgressRequest request = new WorkOrderProgressRequest();
        request.setProcessName("备料");
        request.setProgress(80);
        request.setCompleteCurrentProcess(false);

        assertThrows(BusinessException.class, () -> workOrderService.updateProgress(1L, request));
    }

    @Test
    void applySchedulingSuggestions_shouldWriteBackTeamAndPriority() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-001");
        order.setStatus("pending");
        order.setPriority(2);

        ProdTeam team = new ProdTeam();
        team.setId(2L);
        team.setTeamName("甲班");

        when(prodWorkOrderMapper.selectOne(any())).thenReturn(order);
        when(prodTeamMapper.selectOne(any())).thenReturn(team);

        SchedulingApplyRequest request = new SchedulingApplyRequest();
        request.setPlanDate(LocalDate.of(2026, 8, 19));
        request.setSummary("建议优先排产 WO-001");

        SchedulingDispatchItem dispatch = new SchedulingDispatchItem();
        dispatch.setWorkOrderCode("WO-001");
        dispatch.setTeamName("甲班");
        dispatch.setStartTime("2026-08-19 08:00");
        dispatch.setHours("4");
        request.setDispatches(List.of(dispatch));

        SchedulingPriorityItem priority = new SchedulingPriorityItem();
        priority.setWorkOrderCode("WO-001");
        priority.setPriorityLabel("高");
        priority.setReason("交期较近");
        request.setPriorities(List.of(priority));

        Map<String, Object> result = workOrderService.applySchedulingSuggestions(request);

        assertEquals(1, result.get("appliedCount"));
        assertEquals(0, result.get("skippedCount"));
        assertEquals("assigned", order.getStatus());
        assertEquals(2L, order.getTeamId());
        assertEquals(1, order.getPriority());
        verify(prodWorkOrderMapper).updateById(order);
    }

    @Test
    void applySchedulingSuggestions_shouldSkipMissingWorkOrder() {
        when(prodWorkOrderMapper.selectOne(any())).thenReturn(null);

        SchedulingApplyRequest request = new SchedulingApplyRequest();
        SchedulingDispatchItem dispatch = new SchedulingDispatchItem();
        dispatch.setWorkOrderCode("WO-MISSING");
        dispatch.setTeamName("甲班");
        request.setDispatches(List.of(dispatch));

        Map<String, Object> result = workOrderService.applySchedulingSuggestions(request);

        assertEquals(0, result.get("appliedCount"));
        assertEquals(1, result.get("skippedCount"));
    }

    @Test
    void applySchedulingSuggestions_shouldSkipUnknownTeam() {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-002");
        when(prodWorkOrderMapper.selectOne(any())).thenReturn(order);
        when(prodTeamMapper.selectOne(any())).thenReturn(null);

        SchedulingApplyRequest request = new SchedulingApplyRequest();
        SchedulingDispatchItem dispatch = new SchedulingDispatchItem();
        dispatch.setWorkOrderCode("WO-002");
        dispatch.setTeamName("未知班组");
        request.setDispatches(List.of(dispatch));

        Map<String, Object> result = workOrderService.applySchedulingSuggestions(request);

        assertEquals(0, result.get("appliedCount"));
        assertEquals(1, result.get("skippedCount"));
    }

    private SysUser adminUser() {
        SysUser user = new SysUser();
        user.setId(99L);
        user.setRole("admin");
        return user;
    }

    private ProdWorkOrder producingOrder(String processName) {
        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(1L);
        order.setOrderNo("WO-PROGRESS");
        order.setStatus("producing");
        order.setProcessName(processName);
        order.setProgress(0);
        order.setProductId(10L);
        order.setProductName("测试产品");
        return order;
    }

    private ProdProcessRecord processRecord(Long id, int seqNo, String name, String status) {
        ProdProcessRecord record = new ProdProcessRecord();
        record.setId(id);
        record.setWorkOrderId(1L);
        record.setSeqNo(seqNo);
        record.setProcessName(name);
        record.setStatus(status);
        return record;
    }

    private void stubDetailQueries(ProdWorkOrder order) {
        lenient().when(prodPlanMapper.selectById(any())).thenReturn(null);
        lenient().when(prodTeamMapper.selectById(any())).thenReturn(null);
        lenient().when(excEventMapper.selectCount(any())).thenReturn(0L);
        lenient().when(prodProcessRecordMapper.selectList(any())).thenReturn(List.of());
    }
}

