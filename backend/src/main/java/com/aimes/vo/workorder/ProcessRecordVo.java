package com.aimes.vo.workorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRecordVo {

    private Long id;
    private Long workOrderId;
    private Integer seqNo;
    private String operationName;
    private String status;
    private Integer progress;
    private Long claimUserId;
    private String claimUserName;
    private Long deviceId;
    private String deviceName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String remark;
}
