package com.aimes.vo.plan;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanBriefVo {

    private Long id;
    private String planNo;
    private String productName;
    private Integer planQty;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    private String status;
    private String statusLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;
}
