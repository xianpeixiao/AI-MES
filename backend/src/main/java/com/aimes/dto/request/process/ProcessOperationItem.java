package com.aimes.dto.request.process;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProcessOperationItem {
    private Long id;
    @NotNull(message = "工序序号不能为空")
    private Integer seqNo;
    private String operationCode;
    @NotBlank(message = "工序名称不能为空")
    private String operationName;
    private Double standardHours;
    private Double prepHours;
    private Double changeoverHours;
    private Integer needReport;
    private Integer needCheck;
    private Integer needScan;
    private String remark;
    private List<ProcessParameterItem> parameters;
    private List<Long> deviceIds;
    private List<Long> categoryIds;
    private List<ProcessMaterialItem> materials;
}
