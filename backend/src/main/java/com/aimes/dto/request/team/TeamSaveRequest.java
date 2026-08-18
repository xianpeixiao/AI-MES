package com.aimes.dto.request.team;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamSaveRequest {
    private String teamCode;
    @NotBlank(message = "班组名称不能为空")
    private String teamName;
    private Long leaderId;
    @Min(value = 0, message = "成员数不能小于 0")
    private Integer memberCount;
    private String lineName;
}
