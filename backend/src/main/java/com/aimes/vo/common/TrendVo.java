package com.aimes.vo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendVo {

    /** Percentage change value (e.g. 100 for +100%). */
    private Long value;

    /** Trend direction: up, down, or flat. */
    private String direction;

    /** Display label (e.g. "+100%", "0%"). */
    private String label;
}
