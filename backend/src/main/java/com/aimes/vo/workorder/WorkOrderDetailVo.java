package com.aimes.vo.workorder;

import com.aimes.vo.exception.ExceptionVo;
import com.aimes.vo.product.ProductVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkOrderDetailVo extends WorkOrderVo {

    private List<ProcessRecordVo> processRecords;
    private Map<String, Object> routingContext;
    private List<ExceptionVo> exceptions;
    private ProductVo product;
    private List<Map<String, Object>> bomPreview;
    private Boolean hasBom;
}
