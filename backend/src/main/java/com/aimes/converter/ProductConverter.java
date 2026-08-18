package com.aimes.converter;

import com.aimes.entity.MdmProduct;
import com.aimes.service.ProductService;
import com.aimes.vo.product.ProductDetailVo;
import com.aimes.vo.product.ProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductConverter {

    public ProductVo toVo(MdmProduct product, boolean hasBom) {
        if (product == null) {
            return null;
        }
        return ProductVo.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .spec(product.getSpec())
                .unit(product.getUnit())
                .stockQty(product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO)
                .status(product.getStatus())
                .statusLabel(ProductService.productStatusLabel(product.getStatus()))
                .remark(product.getRemark())
                .hasBom(hasBom)
                .createdTime(product.getCreatedTime())
                .updatedTime(product.getUpdatedTime())
                .build();
    }

    public ProductVo toVo(MdmProduct product) {
        return toVo(product, false);
    }

    public ProductDetailVo toDetailVo(MdmProduct product, java.util.Map<String, Object> bom, boolean hasBom) {
        ProductVo base = toVo(product, hasBom);
        return ProductDetailVo.builder()
                .id(base.getId())
                .productCode(base.getProductCode())
                .productName(base.getProductName())
                .spec(base.getSpec())
                .unit(base.getUnit())
                .stockQty(base.getStockQty())
                .status(base.getStatus())
                .statusLabel(base.getStatusLabel())
                .remark(base.getRemark())
                .hasBom(base.getHasBom())
                .createdTime(base.getCreatedTime())
                .updatedTime(base.getUpdatedTime())
                .bom(bom)
                .build();
    }
}
