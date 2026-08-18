package com.aimes.vo.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductDetailVo extends ProductVo {

    /** 工艺/BOM 聚合视图，阶段二替换为 BomViewVo */
    private Map<String, Object> bom;
}
