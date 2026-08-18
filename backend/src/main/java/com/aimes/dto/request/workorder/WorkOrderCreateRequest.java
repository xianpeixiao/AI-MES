package com.aimes.dto.request.workorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderCreateRequest {
    private Long planId;
    private Long productId;
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    @Min(value = 1, message = "生产数量必须大于 0")
    private Integer orderQty;
    private String orderNo;
    private Long teamId;
    private String processName;
    @Min(value = 0, message = "进度不能小于 0")
    @Max(value = 100, message = "进度不能大于 100")
    private Integer progress;
    private String status;
    @Min(value = 1, message = "优先级最小为 1")
    @Max(value = 3, message = "优先级最大为 3")
    private Integer priority;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    private String remark;
}
