package com.aimes.service;

import com.aimes.entity.InvTransaction;
import com.aimes.entity.MdmProduct;
import com.aimes.entity.SysUser;
import com.aimes.mapper.InvTransactionMapper;
import com.aimes.mapper.MdmProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private MdmProductMapper mdmProductMapper;
    @Mock
    private InvTransactionMapper invTransactionMapper;
    @Mock
    private AuthService authService;
    @Mock
    private ProcessRouteService processRouteService;

    @InjectMocks
    private ProductService productService;

    @Test
    void receiveFromWorkOrder_shouldIncreaseProductStockOnce() {
        SysUser user = new SysUser();
        user.setId(1L);
        when(authService.currentUser()).thenReturn(user);
        when(invTransactionMapper.selectCount(any())).thenReturn(0L);

        MdmProduct product = new MdmProduct();
        product.setId(10L);
        product.setProductName("测试产品");
        product.setStockQty(new BigDecimal("5"));
        when(mdmProductMapper.selectById(10L)).thenReturn(product);

        productService.receiveFromWorkOrder(100L, 10L, 3, "WO-100");

        assertEquals(0, new BigDecimal("8").compareTo(product.getStockQty()));
        verify(mdmProductMapper).updateById(product);

        ArgumentCaptor<InvTransaction> txnCaptor = ArgumentCaptor.forClass(InvTransaction.class);
        verify(invTransactionMapper).insert(txnCaptor.capture());
        InvTransaction txn = txnCaptor.getValue();
        assertEquals("in", txn.getTxnType());
        assertEquals(100L, txn.getRefId());
        assertEquals(10L, txn.getProductId());
    }

    @Test
    void receiveFromWorkOrder_whenAlreadyReceived_shouldSkip() {
        when(invTransactionMapper.selectCount(any())).thenReturn(1L);

        productService.receiveFromWorkOrder(101L, 10L, 2, "WO-101");

        verify(mdmProductMapper, never()).selectById(any());
        verify(invTransactionMapper, never()).insert(any(InvTransaction.class));
    }
}
