package com.aimes.vo.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialListVo {

    private MaterialSummaryVo summary;
    private List<MaterialVo> records;
}
