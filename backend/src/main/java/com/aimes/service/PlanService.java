package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.common.OperationLog;
import com.aimes.common.OperationLogRunner;
import com.aimes.config.AimesProperties;
import com.aimes.converter.PlanConverter;
import com.aimes.converter.ProductConverter;
import com.aimes.dto.request.plan.PlanSaveRequest;
import com.aimes.entity.ProdPlan;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ProdPlanMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.common.PageResult;
import com.aimes.vo.plan.PlanBriefVo;
import com.aimes.vo.plan.PlanDetailVo;
import com.aimes.vo.plan.PlanReleasePreviewVo;
import com.aimes.vo.plan.PlanReleaseResultVo;
import com.aimes.vo.plan.PlanSplitPreviewItemVo;
import com.aimes.vo.plan.PlanSummaryVo;
import com.aimes.vo.plan.PlanVo;
import com.aimes.vo.product.ProductVo;
import com.aimes.vo.workorder.WorkOrderSummaryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final ProdPlanMapper prodPlanMapper;
    private final ProdWorkOrderMapper prodWorkOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final SysNotificationService sysNotificationService;
    private final WorkOrderNoService workOrderNoService;
    private final ReferentialIntegrityService referentialIntegrityService;
    private final ProcessRouteService processRouteService;
    private final OperationLogRunner operationLogRunner;
    private final ProductService productService;
    private final AimesProperties aimesProperties;
    private final PlanConverter planConverter;
    private final ProductConverter productConverter;

    public PageResult<PlanVo> list(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<ProdPlan> wrapper = new LambdaQueryWrapper<ProdPlan>()
                .and(StringUtils.hasText(keyword), w -> w.like(ProdPlan::getPlanNo, keyword)
                        .or()
                        .like(ProdPlan::getProductName, keyword));
        if (StringUtils.hasText(status)) {
            if ("done".equals(status)) {
                wrapper.in(ProdPlan::getStatus, List.of("done", "completed"));
            } else {
                wrapper.eq(ProdPlan::getStatus, status);
            }
        }
        wrapper.orderByDesc(ProdPlan::getPlanDate)
                .orderByDesc(ProdPlan::getId);
        Page<ProdPlan> page = prodPlanMapper.selectPage(new Page<>(current, size), wrapper);
        return planConverter.toPage(page);
    }

    /** 供 Coze 内部上下文使用，保持 Map 结构 */
    public Map<String, Object> summary() {
        List<ProdPlan> plans = prodPlanMapper.selectList(new LambdaQueryWrapper<ProdPlan>()
                .orderByDesc(ProdPlan::getPlanDate)
                .orderByDesc(ProdPlan::getId));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", plans.size());
        summary.put("draftCount", plans.stream().filter(p -> "draft".equals(p.getStatus())).count());
        summary.put("releasedCount", plans.stream().filter(p -> "released".equals(p.getStatus())).count());
        summary.put("completedCount", plans.stream().filter(p -> PlanConverter.isCompletedStatus(p.getStatus())).count());
        summary.put("pausedCount", plans.stream().filter(p -> "paused".equals(p.getStatus())).count());
        summary.put("plans", plans.stream().map(planConverter::toBriefVo).toList());
        return summary;
    }

  public PlanSummaryVo summaryVo() {
        List<ProdPlan> plans = prodPlanMapper.selectList(new LambdaQueryWrapper<ProdPlan>()
                .orderByDesc(ProdPlan::getPlanDate)
                .orderByDesc(ProdPlan::getId));
        return PlanSummaryVo.builder()
                .totalCount(plans.size())
                .draftCount(plans.stream().filter(p -> "draft".equals(p.getStatus())).count())
                .releasedCount(plans.stream().filter(p -> "released".equals(p.getStatus())).count())
                .completedCount(plans.stream().filter(p -> PlanConverter.isCompletedStatus(p.getStatus())).count())
                .pausedCount(plans.stream().filter(p -> "paused".equals(p.getStatus())).count())
                .plans(plans.stream().map(planConverter::toBriefVo).toList())
                .build();
    }

    /** 供 Coze 内部上下文使用 */
    public List<Map<String, Object>> listBriefByStatuses(List<String> statuses, int limit) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return prodPlanMapper.selectList(new LambdaQueryWrapper<ProdPlan>()
                        .in(ProdPlan::getStatus, statuses)
                        .orderByDesc(ProdPlan::getPlanDate)
                        .orderByDesc(ProdPlan::getId)
                        .last("limit " + Math.max(1, limit)))
                .stream()
                .map(plan -> briefToMap(planConverter.toBriefVo(plan)))
                .toList();
    }

    public List<PlanBriefVo> listBriefVoByStatuses(List<String> statuses, int limit) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return prodPlanMapper.selectList(new LambdaQueryWrapper<ProdPlan>()
                        .in(ProdPlan::getStatus, statuses)
                        .orderByDesc(ProdPlan::getPlanDate)
                        .orderByDesc(ProdPlan::getId)
                        .last("limit " + Math.max(1, limit)))
                .stream()
                .map(planConverter::toBriefVo)
                .toList();
    }

    public static boolean isCompletedStatus(ProdPlan plan) {
        return PlanConverter.isCompletedStatus(plan);
    }

    public static boolean isCompletedStatus(String status) {
        return PlanConverter.isCompletedStatus(status);
    }

    public static String planStatusLabel(String status) {
        return PlanConverter.planStatusLabel(status);
    }

    public PlanDetailVo detail(Long id) {
        ProdPlan plan = getPlan(id);
        List<ProdWorkOrder> orders = prodWorkOrderMapper.selectList(new LambdaQueryWrapper<ProdWorkOrder>()
                .eq(ProdWorkOrder::getPlanId, id)
                .orderByDesc(ProdWorkOrder::getId));
        ProductVo product = null;
        Boolean hasBom = null;
        if (plan.getProductId() != null) {
            hasBom = productService.hasActiveBom(plan.getProductId());
            product = productConverter.toVo(productService.requireProduct(plan.getProductId()), hasBom);
        }
        return planConverter.toDetailVo(plan, orders, product, hasBom);
    }

    @Transactional
    public PlanVo create(PlanSaveRequest request) {
        ProdPlan plan = new ProdPlan();
        plan.setPlanNo(StringUtils.hasText(request.getPlanNo()) ? request.getPlanNo() : nextPlanNo());
        applyProduct(plan, request.getProductId(), request.getProductName());
        plan.setPlanQty(request.getPlanQty());
        plan.setPlanDate(request.getPlanDate());
        plan.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");
        plan.setCreatedBy(authService.currentUser().getId());
        plan.setRemark(request.getRemark());
        prodPlanMapper.insert(plan);
        return planConverter.toVo(plan);
    }

    @Transactional
    public PlanVo update(Long id, PlanSaveRequest request) {
        ProdPlan plan = getPlan(id);
        if ("done".equals(plan.getStatus()) || "completed".equals(plan.getStatus())) {
            throw new BusinessException("已完成计划不允许修改");
        }
        applyProduct(plan, request.getProductId(), request.getProductName());
        plan.setPlanQty(request.getPlanQty());
        plan.setPlanDate(request.getPlanDate());
        if (StringUtils.hasText(request.getStatus())) {
            plan.setStatus(request.getStatus());
        }
        plan.setRemark(request.getRemark());
        prodPlanMapper.updateById(plan);
        return planConverter.toVo(plan);
    }

    @Transactional
    @OperationLog(module = "生产计划", action = "删除")
    public void delete(Long id) {
        operationLogRunner.runVoid("生产计划", "删除", "delete", new Object[]{id}, () -> deleteInternal(id));
    }

    private void deleteInternal(Long id) {
        referentialIntegrityService.ensurePlanDeletable(id);
        prodPlanMapper.deleteById(id);
    }

    @Transactional
    @OperationLog(module = "生产计划", action = "下发")
    public PlanReleaseResultVo release(Long id) {
        return operationLogRunner.runUnchecked("生产计划", "下发", "release", new Object[]{id}, () -> releaseInternal(id));
    }

    public PlanReleasePreviewVo previewRelease(Long id) {
        ProdPlan plan = getPlan(id);
        if (!"draft".equals(plan.getStatus())) {
            throw new BusinessException("只有草稿计划可以预览下发");
        }
        List<PlanSplitPreviewItemVo> workOrders = buildSplitPreview(plan);
        boolean hasBom = plan.getProductId() != null && productService.hasActiveBom(plan.getProductId());
        String bomWarning = hasBom ? null : "产品工艺路线未配置工序物料，下发后无法预览理论用料";
        return planConverter.toReleasePreviewVo(
                planConverter.toVo(plan),
                aimesProperties.getPlanSplitQty(),
                workOrders,
                hasBom,
                bomWarning
        );
    }

    private PlanReleaseResultVo releaseInternal(Long id) {
        ProdPlan plan = getPlan(id);
        if (!"draft".equals(plan.getStatus())) {
            throw new BusinessException("只有草稿计划可以下发");
        }

        plan.setStatus("released");
        plan.setReleaseTime(LocalDateTime.now());
        prodPlanMapper.updateById(plan);

        List<SysUser> usersToNotify = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, List.of("admin", "supervisor")));
        for (SysUser recipient : usersToNotify) {
            sysNotificationService.createNotification(
                    recipient.getId(),
                    "计划发布",
                    "生产计划 “" + plan.getPlanNo() + "” 状态已更新为已下发。",
                    "info",
                    "/plans"
            );
        }

        List<Integer> splitQtys = computeSplitQuantities(plan.getPlanQty());
        List<ProdWorkOrder> generated = new ArrayList<>();
        int index = 1;
        for (Integer qty : splitQtys) {
            ProdWorkOrder order = new ProdWorkOrder();
            order.setOrderNo(workOrderNoService.nextOrderNo());
            order.setPlanId(plan.getId());
            order.setProductId(plan.getProductId());
            order.setProductName(plan.getProductName());
            order.setOrderQty(qty);
            processRouteService.applyRoutingToWorkOrder(order, plan.getProductId(), plan.getProductName());
            order.setProgress(0);
            order.setStatus("pending");
            order.setPriority(2);
            order.setDeadline(plan.getPlanDate().atTime(18, 0));
            String splitHint = splitQtys.size() > 1 ? "（批次 " + index + "/" + splitQtys.size() + "，数量 " + qty + "）" : "";
            order.setRemark("由计划 " + plan.getPlanNo() + " 自动生成" + splitHint);
            prodWorkOrderMapper.insert(order);
            processRouteService.initProcessRecords(order.getId(), plan.getProductId(), plan.getProductName());
            generated.add(order);
            index++;
        }

        List<WorkOrderSummaryVo> summaries = generated.stream()
                .map(planConverter::toWorkOrderSummary)
                .toList();
        return planConverter.toReleaseResultVo(
                planConverter.toVo(plan),
                summaries,
                plan.getProductId() != null && productService.hasActiveBom(plan.getProductId())
        );
    }

    private List<PlanSplitPreviewItemVo> buildSplitPreview(ProdPlan plan) {
        List<Integer> splitQtys = computeSplitQuantities(plan.getPlanQty());
        List<PlanSplitPreviewItemVo> rows = new ArrayList<>();
        int index = 1;
        for (Integer qty : splitQtys) {
            rows.add(planConverter.toSplitPreviewItem(
                    index, qty, plan.getProductName(), plan.getPlanDate().atTime(18, 0)));
            index++;
        }
        return rows;
    }

    private List<Integer> computeSplitQuantities(int planQty) {
        int splitSize = Math.max(1, aimesProperties.getPlanSplitQty());
        List<Integer> quantities = new ArrayList<>();
        int remaining = planQty;
        while (remaining > 0) {
            int batch = Math.min(remaining, splitSize);
            quantities.add(batch);
            remaining -= batch;
        }
        return quantities;
    }

    private void applyProduct(ProdPlan plan, Long productId, String productName) {
        if (productId != null) {
            var product = productService.requireProduct(productId);
            plan.setProductId(productId);
            plan.setProductName(product.getProductName());
            return;
        }
        plan.setProductName(productName);
        var product = productService.findByName(productName);
        if (product != null) {
            plan.setProductId(product.getId());
        } else {
            plan.setProductId(null);
        }
    }

    private ProdPlan getPlan(Long id) {
        ProdPlan plan = prodPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("生产计划不存在");
        }
        return plan;
    }

    private Map<String, Object> briefToMap(PlanBriefVo brief) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", brief.getId());
        map.put("planNo", brief.getPlanNo());
        map.put("productName", brief.getProductName());
        map.put("planQty", brief.getPlanQty());
        map.put("planDate", brief.getPlanDate());
        map.put("status", brief.getStatus());
        map.put("statusLabel", brief.getStatusLabel());
        map.put("releaseTime", brief.getReleaseTime());
        return map;
    }

    private String nextPlanNo() {
        long count = prodPlanMapper.selectCount(null) + 1;
        return "PLAN-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", count);
    }
}

