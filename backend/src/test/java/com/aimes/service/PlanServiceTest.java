package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.common.OperationLogRunner;
import com.aimes.config.AimesProperties;
import com.aimes.converter.PlanConverter;
import com.aimes.entity.ProdPlan;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdPlanMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.plan.PlanReleasePreviewVo;
import com.aimes.vo.plan.PlanReleaseResultVo;
import com.aimes.vo.plan.PlanSplitPreviewItemVo;
import com.aimes.vo.plan.PlanVo;
import com.aimes.vo.workorder.WorkOrderSummaryVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanServiceTest {

    @Mock
    private ProdPlanMapper prodPlanMapper;
    @Mock
    private ProdWorkOrderMapper prodWorkOrderMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private AuthService authService;
    @Mock
    private SysNotificationService sysNotificationService;
    @Mock
    private WorkOrderNoService workOrderNoService;
    @Mock
    private ReferentialIntegrityService referentialIntegrityService;
    @Mock
    private ProcessRouteService processRouteService;
    @Mock
    private OperationLogRunner operationLogRunner;
    @Mock
    private ProductService productService;
    @Mock
    private AimesProperties aimesProperties;
    @Mock
    private PlanConverter planConverter;

    @InjectMocks
    private PlanService planService;

    @BeforeEach
    void setUpRunner() throws Exception {
        when(operationLogRunner.runUnchecked(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(4);
            return callable.call();
        });
    }

    @Test
    void release_shouldRejectNonDraftPlan() {
        ProdPlan plan = sampleDraftPlan(100);
        plan.setStatus("released");
        when(prodPlanMapper.selectById(1L)).thenReturn(plan);

        assertThrows(BusinessException.class, () -> planService.release(1L));
        verify(prodWorkOrderMapper, times(0)).insert(any(ProdWorkOrder.class));
    }

    @Test
    void release_shouldSplitPlanIntoMultipleWorkOrders() {
        ProdPlan plan = sampleDraftPlan(250);
        when(prodPlanMapper.selectById(1L)).thenReturn(plan);
        when(aimesProperties.getPlanSplitQty()).thenReturn(100);
        when(workOrderNoService.nextOrderNo()).thenReturn("WO-001", "WO-002", "WO-003");
        when(sysUserMapper.selectList(any())).thenReturn(List.of(new SysUser()));
        when(productService.hasActiveBom(10L)).thenReturn(true);
        when(planConverter.toVo(plan)).thenReturn(PlanVo.builder().id(1L).planNo("PLAN-2026-001").build());
        when(planConverter.toWorkOrderSummary(any(ProdWorkOrder.class))).thenAnswer(invocation -> {
            ProdWorkOrder order = invocation.getArgument(0);
            return WorkOrderSummaryVo.builder()
                    .orderNo(order.getOrderNo())
                    .orderQty(order.getOrderQty())
                    .build();
        });
        when(planConverter.toReleaseResultVo(any(), any(), eq(true))).thenAnswer(invocation ->
                PlanReleaseResultVo.builder()
                        .plan(invocation.getArgument(0))
                        .generatedWorkOrders(invocation.getArgument(1))
                        .hasBom(true)
                        .build());

        PlanReleaseResultVo result = planService.release(1L);

        assertEquals("released", plan.getStatus());
        assertEquals(3, result.getGeneratedWorkOrders().size());

        ArgumentCaptor<ProdWorkOrder> orderCaptor = ArgumentCaptor.forClass(ProdWorkOrder.class);
        verify(prodWorkOrderMapper, times(3)).insert(orderCaptor.capture());
        assertEquals(List.of(100, 100, 50), orderCaptor.getAllValues().stream().map(ProdWorkOrder::getOrderQty).toList());
        verify(processRouteService, times(3)).initProcessRecords(any(), eq(10L), eq("测试产品"));
    }

    @Test
    void previewRelease_shouldReturnSplitPreviewForDraftPlan() {
        ProdPlan plan = sampleDraftPlan(150);
        when(prodPlanMapper.selectById(1L)).thenReturn(plan);
        when(aimesProperties.getPlanSplitQty()).thenReturn(100);
        when(productService.hasActiveBom(10L)).thenReturn(false);
        when(planConverter.toVo(plan)).thenReturn(PlanVo.builder().id(1L).planNo("PLAN-2026-002").build());
        when(planConverter.toSplitPreviewItem(anyInt(), anyInt(), any(), any())).thenAnswer(invocation ->
                PlanSplitPreviewItemVo.builder()
                        .batchNo(invocation.getArgument(0))
                        .quantity(invocation.getArgument(1))
                        .productName(invocation.getArgument(2))
                        .deadline(invocation.getArgument(3))
                        .build());
        when(planConverter.toReleasePreviewVo(any(), eq(100), any(), eq(false), any())).thenAnswer(invocation ->
                PlanReleasePreviewVo.builder()
                        .plan(invocation.getArgument(0))
                        .splitQty(100)
                        .workOrderCount(((List<?>) invocation.getArgument(2)).size())
                        .workOrders(invocation.getArgument(2))
                        .hasBom(false)
                        .bomWarning(invocation.getArgument(4))
                        .build());

        PlanReleasePreviewVo preview = planService.previewRelease(1L);

        assertEquals(2, preview.getWorkOrderCount());
        assertEquals(false, preview.getHasBom());
    }

    private ProdPlan sampleDraftPlan(int planQty) {
        ProdPlan plan = new ProdPlan();
        plan.setId(1L);
        plan.setPlanNo("PLAN-2026-001");
        plan.setProductId(10L);
        plan.setProductName("测试产品");
        plan.setPlanQty(planQty);
        plan.setPlanDate(LocalDate.of(2026, 8, 19));
        plan.setStatus("draft");
        return plan;
    }
}
