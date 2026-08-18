package com.aimes.converter;

import com.aimes.entity.DevDevice;
import com.aimes.entity.ExcEvent;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.SysUser;
import com.aimes.vo.exception.ExceptionVo;
import org.springframework.stereotype.Component;

@Component
public class ExceptionConverter {

    public ExceptionVo toVo(ExcEvent event, ProdWorkOrder workOrder, DevDevice device,
                            SysUser reporter, SysUser handler) {
        if (event == null) {
            return null;
        }
        return ExceptionVo.builder()
                .id(event.getId())
                .eventNo(event.getEventNo())
                .eventType(event.getEventType())
                .workOrderId(event.getWorkOrderId())
                .workOrderNo(workOrder == null ? null : workOrder.getOrderNo())
                .deviceId(event.getDeviceId())
                .deviceCode(device == null ? null : device.getDeviceCode())
                .deviceName(device == null ? null : device.getDeviceName())
                .description(event.getDescription())
                .status(event.getStatus())
                .reporterId(event.getReporterId())
                .reporterName(reporter == null ? null : reporter.getRealName())
                .handlerId(event.getHandlerId())
                .handlerName(handler == null ? null : handler.getRealName())
                .occurTime(event.getOccurTime())
                .createTime(event.getCreateTime())
                .handleTime(event.getHandleTime())
                .handleAction(event.getHandleAction())
                .handleResult(event.getHandleResult())
                .build();
    }
}
