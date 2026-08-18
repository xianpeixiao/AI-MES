package com.aimes.converter;

import com.aimes.entity.ProdPlan;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.common.PageResult;
import com.aimes.vo.plan.PlanBriefVo;
import com.aimes.vo.plan.PlanDetailVo;
import com.aimes.vo.plan.PlanReleasePreviewVo;
import com.aimes.vo.plan.PlanReleaseResultVo;
import com.aimes.vo.plan.PlanSplitPreviewItemVo;
import com.aimes.vo.plan.PlanVo;
import com.aimes.vo.product.ProductVo;
import com.aimes.vo.workorder.WorkOrderSummaryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanConverter {

    private final SysUserMapper sysUserMapper;
    private final ProdWorkOrderMapper prodWorkOrderMapper;

    public PageResult<PlanVo> toPage(Page<ProdPlan> page) {
        List<PlanVo> records = page.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(page.getTotal(), records);
    }

    public PlanVo toVo(ProdPlan plan) {
        if (plan == null) {
            return null;
        }
        SysUser creator = plan.getCreatedBy() == null ? null : sysUserMapper.selectById(plan.getCreatedBy());
        long workOrderCount = prodWorkOrderMapper.selectCount(new LambdaQueryWrapper<ProdWorkOrder>()
                .eq(ProdWorkOrder::getPlanId, plan.getId()));
        return PlanVo.builder()
                .id(plan.getId())
                .planNo(plan.getPlanNo())
                .productId(plan.getProductId())
                .productName(plan.getProductName())
                .planQty(plan.getPlanQty())
                .planDate(plan.getPlanDate())
                .status(plan.getStatus())
                .remark(plan.getRemark())
                .createdBy(plan.getCreatedBy())
                .createdByName(creator == null ? null : creator.getRealName())
                .releaseTime(plan.getReleaseTime())
                .createdTime(plan.getCreatedTime())
                .updatedTime(plan.getUpdatedTime())
                .workOrderCount(workOrderCount)
                .build();
    }

    public PlanBriefVo toBriefVo(ProdPlan plan) {
        if (plan == null) {
            return null;
        }
        return PlanBriefVo.builder()
                .id(plan.getId())
                .planNo(plan.getPlanNo())
                .productName(plan.getProductName())
                .planQty(plan.getPlanQty())
                .planDate(plan.getPlanDate())
                .status(plan.getStatus())
                .statusLabel(planStatusLabel(plan.getStatus()))
                .releaseTime(plan.getReleaseTime())
                .build();
    }

    public PlanDetailVo toDetailVo(ProdPlan plan, List<ProdWorkOrder> orders, ProductVo product, Boolean hasBom) {
        PlanVo base = toVo(plan);
        return PlanDetailVo.builder()
                .id(base.getId())
                .planNo(base.getPlanNo())
                .productId(base.getProductId())
                .productName(base.getProductName())
                .planQty(base.getPlanQty())
                .planDate(base.getPlanDate())
                .status(base.getStatus())
                .remark(base.getRemark())
                .createdBy(base.getCreatedBy())
                .createdByName(base.getCreatedByName())
                .releaseTime(base.getReleaseTime())
                .createdTime(base.getCreatedTime())
                .updatedTime(base.getUpdatedTime())
                .workOrderCount(base.getWorkOrderCount())
                .workOrders(orders.stream().map(this::toWorkOrderSummary).toList())
                .completionProgress(computeCompletionProgress(orders))
                .executionStatus(resolveExecutionStatus(plan, orders))
                .product(product)
                .hasBom(hasBom)
                .build();
    }

    public PlanReleasePreviewVo toReleasePreviewVo(PlanVo plan, int splitQty, List<PlanSplitPreviewItemVo> workOrders,
                                                   boolean hasBom, String bomWarning) {
        return PlanReleasePreviewVo.builder()
                .plan(plan)
                .splitQty(splitQty)
                .workOrderCount(workOrders.size())
                .workOrders(workOrders)
                .hasBom(hasBom)
                .bomWarning(bomWarning)
                .build();
    }

    public PlanReleaseResultVo toReleaseResultVo(PlanVo plan, List<WorkOrderSummaryVo> generated, Boolean hasBom) {
        return PlanReleaseResultVo.builder()
                .plan(plan)
                .generatedWorkOrders(generated)
                .generatedWorkOrder(generated.isEmpty() ? null : generated.get(0))
                .hasBom(hasBom)
                .build();
    }

    public PlanSplitPreviewItemVo toSplitPreviewItem(int batchNo, int quantity, String productName,
                                                     java.time.LocalDateTime deadline) {
        return PlanSplitPreviewItemVo.builder()
                .batchNo(batchNo)
                .quantity(quantity)
                .productName(productName)
                .deadline(deadline)
                .build();
    }

    public WorkOrderSummaryVo toWorkOrderSummary(ProdWorkOrder order) {
        if (order == null) {
            return null;
        }
        return WorkOrderSummaryVo.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .orderQty(order.getOrderQty())
                .processName(order.getProcessName())
                .progress(order.getProgress())
                .status(order.getStatus())
                .teamName(null)
                .deadline(order.getDeadline())
                .scheduledStartTime(order.getScheduledStartTime())
                .estimatedHours(order.getEstimatedHours())
                .schedulingRank(order.getSchedulingRank())
                .build();
    }

    public WorkOrderSummaryVo toWorkOrderSummary(ProdWorkOrder order, String teamName) {
        WorkOrderSummaryVo vo = toWorkOrderSummary(order);
        if (vo != null) {
            vo.setTeamName(teamName);
        }
        return vo;
    }

    public static String planStatusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "draft" -> "草稿";
            case "released" -> "已下发";
            case "done", "completed" -> "已完成";
            case "paused" -> "已暂停";
            case "in_progress" -> "进行中";
            default -> status;
        };
    }

    public static boolean isCompletedStatus(ProdPlan plan) {
        return plan != null && isCompletedStatus(plan.getStatus());
    }

    public static boolean isCompletedStatus(String status) {
        return "done".equals(status) || "completed".equals(status);
    }

    private int computeCompletionProgress(List<ProdWorkOrder> orders) {
        if (orders.isEmpty()) {
            return 0;
        }
        return (int) Math.round(orders.stream()
                .mapToInt(o -> o.getProgress() == null ? 0 : o.getProgress())
                .average()
                .orElse(0));
    }

    private String resolveExecutionStatus(ProdPlan plan, List<ProdWorkOrder> orders) {
        if ("draft".equals(plan.getStatus())) {
            return "draft";
        }
        if ("done".equals(plan.getStatus()) || "completed".equals(plan.getStatus())) {
            return "done";
        }
        if (orders.isEmpty()) {
            return "released";
        }
        boolean allDone = orders.stream().allMatch(o -> "done".equals(o.getStatus()));
        if (allDone) {
            return "done";
        }
        boolean anyStarted = orders.stream().anyMatch(o ->
                ("done".equals(o.getStatus()) || "producing".equals(o.getStatus()) || "exception".equals(o.getStatus()))
                        || (o.getProgress() != null && o.getProgress() > 0));
        return anyStarted ? "in_progress" : "released";
    }
}
