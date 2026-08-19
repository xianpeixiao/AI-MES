package com.aimes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_coze_config")
public class SysCozeConfig {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String apiToken;
    private String botId;
    private String apiUrl;
    private String workflowId;
    private String welcomeMessage;
    private Integer enabled;
    /** coze | deepseek | auto */
    private String aiProvider;
    private String deepseekApiKey;
    private String deepseekApiUrl;
    private String deepseekModel;
    private LocalDateTime updateTime;
}
