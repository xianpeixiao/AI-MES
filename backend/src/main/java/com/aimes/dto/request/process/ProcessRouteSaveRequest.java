package com.aimes.dto.request.process;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProcessRouteSaveRequest {
    private String routeCode;
    @NotBlank(message = "路线名称不能为空")
    private String routeName;
    private Long productId;
    private String productName;
    private String remark;
    /** draft | submit | publish */
    private String saveMode;
    /** 可选；为空时仅保存路线元数据（草稿），不更新工序 */
    private List<ProcessOperationItem> operations;
}
