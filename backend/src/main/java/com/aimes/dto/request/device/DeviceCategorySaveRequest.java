package com.aimes.dto.request.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceCategorySaveRequest {
    private Long parentId;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    private Integer sortNo;
}
