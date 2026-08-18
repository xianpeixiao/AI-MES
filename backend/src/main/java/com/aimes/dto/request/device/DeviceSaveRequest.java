package com.aimes.dto.request.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DeviceSaveRequest {
    private String deviceCode;
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;
    private Long categoryId;
    private String deviceType;
    private String brand;
    private String model;
    private String serialNumber;
    private String workshop;
    private String lineName;
    private String station;
    private Long managerId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate installDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enableDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyDate;
    private String status;
    private Long teamId;
    private String remark;
}
