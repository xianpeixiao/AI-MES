package com.aimes.dto.request.product;

import lombok.Data;

import java.util.List;

@Data
public class BomSaveRequest {
    private String version;
    private String remark;
    private List<BomItemRequest> items;
}
