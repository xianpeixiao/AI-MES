package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.converter.MaterialConverter;
import com.aimes.dto.request.material.MaterialUpdateRequest;
import com.aimes.entity.InvTransaction;
import com.aimes.entity.MatMaterial;
import com.aimes.entity.SysUser;
import com.aimes.mapper.InvTransactionMapper;
import com.aimes.mapper.MatMaterialMapper;
import com.aimes.mapper.SysUserMapper;
import com.aimes.vo.material.MaterialVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialServiceTest {

    @Mock
    private MatMaterialMapper matMaterialMapper;
    @Mock
    private InvTransactionMapper invTransactionMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysNotificationService sysNotificationService;
    @Mock
    private AuthService authService;
    @Mock
    private MaterialConverter materialConverter;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void pickForWorkOrder_shouldDeductStockAndWritePickTransaction() {
        SysUser user = new SysUser();
        user.setId(9L);
        when(authService.currentUser()).thenReturn(user);
        when(invTransactionMapper.selectCount(any())).thenReturn(0L);

        MatMaterial material = sampleMaterial(1L, "螺丝", new BigDecimal("100"), new BigDecimal("20"));
        when(matMaterialMapper.selectById(1L)).thenReturn(material);
        when(materialConverter.toVo(any())).thenReturn(new MaterialVo());

        materialService.pickForWorkOrder(
                100L,
                10L,
                5,
                List.of(Map.of("materialId", 1L, "requiredQty", 30)),
                "bom");

        assertEquals(new BigDecimal("70"), material.getStockQty());
        assertEquals("normal", material.getAlertStatus());

        ArgumentCaptor<InvTransaction> txnCaptor = ArgumentCaptor.forClass(InvTransaction.class);
        verify(invTransactionMapper).insert(txnCaptor.capture());
        InvTransaction txn = txnCaptor.getValue();
        assertEquals("pick", txn.getTxnType());
        assertEquals(new BigDecimal("30"), txn.getQty());
        assertEquals("work_order", txn.getRefType());
        assertEquals(100L, txn.getRefId());
    }

    @Test
    void pickForWorkOrder_whenStockInsufficient_shouldThrow() {
        when(authService.currentUser()).thenReturn(new SysUser());
        when(invTransactionMapper.selectCount(any())).thenReturn(0L);

        MatMaterial material = sampleMaterial(2L, "轴承", new BigDecimal("5"), new BigDecimal("10"));
        when(matMaterialMapper.selectById(2L)).thenReturn(material);

        assertThrows(BusinessException.class, () -> materialService.pickForWorkOrder(
                101L,
                10L,
                1,
                List.of(Map.of("materialId", 2L, "requiredQty", 8))));
    }

    @Test
    void pickForWorkOrder_whenAlreadyPicked_shouldSkip() {
        when(invTransactionMapper.selectCount(any())).thenReturn(1L);

        materialService.pickForWorkOrder(
                102L,
                10L,
                1,
                List.of(Map.of("materialId", 1L, "requiredQty", 1)));

        verify(matMaterialMapper, never()).selectById(any());
        verify(invTransactionMapper, never()).insert(any(InvTransaction.class));
    }

    @Test
    void update_whenStockFallsBelowSafety_shouldMarkWarning() {
        when(authService.currentUser()).thenReturn(userWithId(9L));
        MatMaterial material = sampleMaterial(3L, "垫片", new BigDecimal("30"), new BigDecimal("25"));
        when(matMaterialMapper.selectById(3L)).thenReturn(material);
        when(materialConverter.toVo(material)).thenReturn(new MaterialVo());

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setStockQty(20.0);

        materialService.update(3L, request);

        assertEquals(0, new BigDecimal("20").compareTo(material.getStockQty()));
        assertEquals("warning", material.getAlertStatus());
        verify(matMaterialMapper).updateById(material);
    }

    private SysUser userWithId(long id) {
        SysUser user = new SysUser();
        user.setId(id);
        return user;
    }

    private MatMaterial sampleMaterial(Long id, String name, BigDecimal stock, BigDecimal safety) {
        MatMaterial material = new MatMaterial();
        material.setId(id);
        material.setMaterialCode("MAT-" + id);
        material.setMaterialName(name);
        material.setStockQty(stock);
        material.setSafetyStock(safety);
        material.setAlertStatus(stock.compareTo(safety) < 0 ? "warning" : "normal");
        material.setUnit("件");
        return material;
    }
}
