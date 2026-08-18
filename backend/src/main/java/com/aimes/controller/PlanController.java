package com.aimes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aimes.common.Result;
import com.aimes.dto.request.plan.PlanSaveRequest;
import com.aimes.service.PlanService;
import com.aimes.vo.common.PageResult;
import com.aimes.vo.plan.PlanDetailVo;
import com.aimes.vo.plan.PlanReleasePreviewVo;
import com.aimes.vo.plan.PlanReleaseResultVo;
import com.aimes.vo.plan.PlanVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "生产计划")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @SaCheckPermission("生产计划")
    public Result<PageResult<PlanVo>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status) {
        return Result.ok(planService.list(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("生产计划")
    public Result<PlanDetailVo> detail(@PathVariable Long id) {
        return Result.ok(planService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("生产计划")
    public Result<PlanVo> create(@Valid @RequestBody PlanSaveRequest request) {
        return Result.ok(planService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("生产计划")
    public Result<PlanVo> update(@PathVariable Long id, @Valid @RequestBody PlanSaveRequest request) {
        return Result.ok(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("生产计划")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.ok("删除成功", null);
    }

    @GetMapping("/{id}/release-preview")
    @SaCheckPermission("生产计划")
    public Result<PlanReleasePreviewVo> releasePreview(@PathVariable Long id) {
        return Result.ok(planService.previewRelease(id));
    }

    @PostMapping("/{id}/release")
    @SaCheckPermission("生产计划")
    public Result<PlanReleaseResultVo> release(@PathVariable Long id) {
        return Result.ok(planService.release(id));
    }
}
