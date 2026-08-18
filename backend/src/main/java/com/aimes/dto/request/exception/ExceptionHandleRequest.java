package com.aimes.dto.request.exception;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExceptionHandleRequest {
    @NotBlank(message = "处理措施不能为空")
    private String handleAction;
    @NotBlank(message = "处理结果不能为空")
    private String handleResult;
}
