package com.aimes.service;

import com.aimes.dto.request.quality.InspectionItemRequest;
import com.aimes.dto.request.quality.InspectionSubmitRequest;
import com.aimes.entity.ExcEvent;
import com.aimes.entity.ProdProcessRecord;
import com.aimes.entity.ProdWorkOrder;
import com.aimes.entity.QmsInspectionPlan;
import com.aimes.entity.QmsInspectionRecord;
import com.aimes.entity.SysUser;
import com.aimes.mapper.ExcEventMapper;
import com.aimes.mapper.MdmOperationMapper;
import com.aimes.mapper.ProdProcessRecordMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.QmsInspectionPlanMapper;
import com.aimes.mapper.QmsInspectionRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QmsInspectionServiceTest {

    @Mock
    private QmsInspectionPlanMapper qmsInspectionPlanMapper;
    @Mock
    private QmsInspectionRecordMapper qmsInspectionRecordMapper;
    @Mock
    private MdmOperationMapper mdmOperationMapper;
    @Mock
    private ProdWorkOrderMapper prodWorkOrderMapper;
    @Mock
    private ProdProcessRecordMapper prodProcessRecordMapper;
    @Mock
    private ExcEventMapper excEventMapper;
    @Mock
    private AuthService authService;

    @InjectMocks
    private QmsInspectionService qmsInspectionService;

    @Test
    void submitInspections_whenAllPass_shouldNotCreateException() {
        stubSubmitContext();

        InspectionSubmitRequest request = baseRequest();
        InspectionItemRequest item = new InspectionItemRequest();
        item.setItemName("外观");
        item.setMeasuredValue("OK");
        request.setItems(List.of(item));

        Map<String, Object> result = qmsInspectionService.submitInspections(request);

        assertFalse((Boolean) result.get("hasFailure"));
        assertNull(result.get("exceptionId"));
        verify(excEventMapper, never()).insert(any(ExcEvent.class));
        verify(prodWorkOrderMapper, never()).updateById(any(ProdWorkOrder.class));
    }

    @Test
    void submitInspections_whenExplicitFail_shouldCreateQualityException() {
        stubSubmitContext();

        InspectionSubmitRequest request = baseRequest();
        InspectionItemRequest item = new InspectionItemRequest();
        item.setItemName("尺寸");
        item.setResult("fail");
        request.setItems(List.of(item));

        Map<String, Object> result = qmsInspectionService.submitInspections(request);

        assertTrue((Boolean) result.get("hasFailure"));
        assertNotNull(result.get("exceptionId"));

        ArgumentCaptor<ExcEvent> eventCaptor = ArgumentCaptor.forClass(ExcEvent.class);
        verify(excEventMapper).insert(eventCaptor.capture());
        assertEquals("quality", eventCaptor.getValue().getEventType());

        ArgumentCaptor<ProdWorkOrder> orderCaptor = ArgumentCaptor.forClass(ProdWorkOrder.class);
        verify(prodWorkOrderMapper).updateById(orderCaptor.capture());
        assertEquals("exception", orderCaptor.getValue().getStatus());
    }

    @Test
    void submitInspections_whenMeasuredOutOfRange_shouldFailAutomatically() {
        stubSubmitContext();

        QmsInspectionPlan plan = new QmsInspectionPlan();
        plan.setId(7L);
        plan.setMinValue("10");
        plan.setMaxValue("20");
        when(qmsInspectionPlanMapper.selectById(7L)).thenReturn(plan);

        InspectionSubmitRequest request = baseRequest();
        InspectionItemRequest item = new InspectionItemRequest();
        item.setPlanId(7L);
        item.setItemName("厚度");
        item.setMeasuredValue("25");
        request.setItems(List.of(item));

        Map<String, Object> result = qmsInspectionService.submitInspections(request);

        assertTrue((Boolean) result.get("hasFailure"));
        verify(qmsInspectionRecordMapper).insert(any(QmsInspectionRecord.class));
        verify(excEventMapper).insert(any(ExcEvent.class));
    }

    @Test
    void submitInspections_whenMeasuredWithinRange_shouldPass() {
        stubSubmitContext();

        QmsInspectionPlan plan = new QmsInspectionPlan();
        plan.setId(8L);
        plan.setMinValue("10");
        plan.setMaxValue("20");
        when(qmsInspectionPlanMapper.selectById(8L)).thenReturn(plan);

        InspectionSubmitRequest request = baseRequest();
        InspectionItemRequest item = new InspectionItemRequest();
        item.setPlanId(8L);
        item.setItemName("厚度");
        item.setMeasuredValue("15");
        request.setItems(List.of(item));

        Map<String, Object> result = qmsInspectionService.submitInspections(request);

        assertFalse((Boolean) result.get("hasFailure"));
        verify(excEventMapper, never()).insert(any(ExcEvent.class));
    }

    private InspectionSubmitRequest baseRequest() {
        InspectionSubmitRequest request = new InspectionSubmitRequest();
        request.setWorkOrderId(100L);
        request.setProcessName("检测");
        return request;
    }

    private void stubSubmitContext() {
        SysUser user = new SysUser();
        user.setId(5L);

        ProdWorkOrder order = new ProdWorkOrder();
        order.setId(100L);
        order.setOrderNo("WO-QMS");
        order.setStatus("producing");

        ProdProcessRecord record = new ProdProcessRecord();
        record.setId(200L);
        record.setWorkOrderId(100L);
        record.setProcessName("检测");
        record.setOperationId(300L);

        when(authService.currentUser()).thenReturn(user);
        when(prodWorkOrderMapper.selectById(100L)).thenReturn(order);
        when(prodProcessRecordMapper.selectOne(any())).thenReturn(record);
        doAnswer(invocation -> {
            QmsInspectionRecord savedRecord = invocation.getArgument(0);
            savedRecord.setId(1000L);
            return 1;
        }).when(qmsInspectionRecordMapper).insert(any(QmsInspectionRecord.class));
        doAnswer(invocation -> {
            ExcEvent event = invocation.getArgument(0);
            event.setId(500L);
            return 1;
        }).when(excEventMapper).insert(any(ExcEvent.class));
        when(excEventMapper.selectCount(any())).thenReturn(0L);
    }
}
