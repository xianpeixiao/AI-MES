package com.aimes.service;

import com.aimes.common.BusinessException;
import com.aimes.entity.ProdPlan;
import com.aimes.entity.ProdTeam;
import com.aimes.entity.SysUser;
import com.aimes.mapper.DevDeviceMapper;
import com.aimes.mapper.ExcEventMapper;
import com.aimes.mapper.ProdPlanMapper;
import com.aimes.mapper.ProdTeamMapper;
import com.aimes.mapper.ProdWorkOrderMapper;
import com.aimes.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferentialIntegrityServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ProdTeamMapper prodTeamMapper;
    @Mock
    private ProdPlanMapper prodPlanMapper;
    @Mock
    private ProdWorkOrderMapper prodWorkOrderMapper;
    @Mock
    private ExcEventMapper excEventMapper;
    @Mock
    private DevDeviceMapper devDeviceMapper;

    @InjectMocks
    private ReferentialIntegrityService referentialIntegrityService;

    @Test
    void ensureTeamDeletable_shouldRejectWhenMembersExist() {
        ProdTeam team = new ProdTeam();
        team.setId(1L);
        when(prodTeamMapper.selectById(1L)).thenReturn(team);
        when(sysUserMapper.selectCount(any())).thenReturn(2L);

        assertThrows(BusinessException.class, () -> referentialIntegrityService.ensureTeamDeletable(1L));
    }

    @Test
    void ensureTeamDeletable_shouldRejectWhenWorkOrdersExist() {
        ProdTeam team = new ProdTeam();
        team.setId(1L);
        when(prodTeamMapper.selectById(1L)).thenReturn(team);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(prodWorkOrderMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> referentialIntegrityService.ensureTeamDeletable(1L));
    }

    @Test
    void ensurePlanDeletable_shouldRejectWhenWorkOrdersExist() {
        ProdPlan plan = new ProdPlan();
        plan.setId(10L);
        when(prodPlanMapper.selectById(10L)).thenReturn(plan);
        when(prodWorkOrderMapper.selectCount(any())).thenReturn(3L);

        assertThrows(BusinessException.class, () -> referentialIntegrityService.ensurePlanDeletable(10L));
    }

    @Test
    void ensureUserDeletable_shouldRejectLastActiveAdmin() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole("admin");
        admin.setStatus(1);
        when(sysUserMapper.selectById(1L)).thenReturn(admin);
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> referentialIntegrityService.ensureUserDeletable(1L));
    }

    @Test
    void ensureUserDeletable_shouldRejectWhenUserCreatedPlans() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setRole("planner");
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        when(prodPlanMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> referentialIntegrityService.ensureUserDeletable(2L));
    }

    @Test
    void ensureUserDeletable_shouldAllowDeletableUser() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setRole("worker");
        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(prodPlanMapper.selectCount(any())).thenReturn(0L);
        when(excEventMapper.selectCount(any())).thenReturn(0L);

        assertDoesNotThrow(() -> referentialIntegrityService.ensureUserDeletable(3L));
    }
}
